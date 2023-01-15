package de.tomsplayground.peanuts.app.yahoo;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.process.PriceProvider;
import de.tomsplayground.peanuts.util.Day;

public class YahooPriceReader extends PriceProvider {

	private final static Logger log = LoggerFactory.getLogger(YahooPriceReader.class);

	private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";

	private static final BigDecimal MAX_VALIDE_PRICE = new BigDecimal("100000");

	public enum Type {
		CURRENT,
		LAST_DAYS,
		HISTORICAL
	}

	private static CloseableHttpClient httpClient;

	private static HttpClientContext context;

	private static boolean isInitialized = false;

	private static void init() {
		if (! isInitialized) {
			RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(StandardCookieSpec.RELAXED).build();
			CookieStore cookieStore = new BasicCookieStore();
			context = HttpClientContext.create();
			context.setCookieStore(cookieStore);
			httpClient = HttpClients.custom().setDefaultRequestConfig(globalConfig).setDefaultCookieStore(cookieStore).build();
			isInitialized = true;
		}
	}

	public static YahooPriceReader forTicker(Security security, Type type) throws IOException {
		init();

		String url;
		if (type == Type.CURRENT) {
			url = "https://query1.finance.yahoo.com/v7/finance/quote?lang=en-US&region=US&corsDomain=finance.yahoo.com&symbols=" +
				URLEncoder.encode(security.getTicker(), StandardCharsets.UTF_8.name());
		} else {
			String range;
			if (type == Type.HISTORICAL) {
				range = "10y";
			} else {
				range = "5d";
			}
			url = "https://query1.finance.yahoo.com/v7/finance/spark?symbols={0}&range={1}&interval=1d";
			url = MessageFormat.format(url,
				URLEncoder.encode(security.getTicker(), StandardCharsets.UTF_8.name()),
				range);
		}
		log.debug("URL: {}", url);
		HttpGet httpGet = new HttpGet(url);
		httpGet.addHeader("User-Agent", USER_AGENT);
		httpGet.setConfig(RequestConfig.custom()
			.setCookieSpec(StandardCookieSpec.RELAXED)
			.setConnectionRequestTimeout(Timeout.ofSeconds(20))
			.build());
		try {
			return httpClient.execute(httpGet, context, response -> {
				String str = EntityUtils.toString(response.getEntity());
				return new YahooPriceReader(security, new StringReader(str), type);
			});
		} catch (IOException e) {
			log.error("URL "+url + " - " + e.getMessage());
			return null;
		}
	}

	public YahooPriceReader(Security security, Reader reader) throws IOException {
		this(security, reader, Type.HISTORICAL);
	}

	public YahooPriceReader(Security security, Reader reader, Type type) throws IOException {
		super(security);
		if (reader == null) {
			return;
		}
		if (type == Type.HISTORICAL || type == Type.LAST_DAYS) {
			readJsonSpark(reader);
		} else {
			readJsonCurrent(reader);
		}
	}

	@SuppressWarnings("unchecked")
	private void readJsonSpark(Reader reader) throws IOException, JsonParseException, JsonMappingException {
		List<IPrice> prices = new ArrayList<>();

		ObjectMapper mapper = new ObjectMapper();
		TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};
		Map<String,Object> jsonMap = mapper.readValue(reader, typeRef);

		jsonMap = (Map<String, Object>) jsonMap.get("spark");
		List<Object> results = (List<Object>) jsonMap.get("result");
		if (results != null) {
			Map<String, Object> result = (Map<String, Object>) results.get(0);
			List<Object> response = (List<Object>) result.get("response");
			Map<String, Object> response1 = (Map<String, Object>) response.get(0);

			List<Integer> timestamps = (List<Integer>) response1.get("timestamp");

			if (timestamps != null) {
				Map<String, Object> indicators = (Map<String, Object>) response1.get("indicators");
				List<Object> quote = (List<Object>) indicators.get("quote");
				List<Double> close = (List<Double>) ((Map<String, Object>)quote.get(0)).get("close");
				for (int i = 0; i < timestamps.size(); i++ ) {
					Integer t = timestamps.get(i);
					Double v = close.get(i);
					if (t != null && v != null) {
						Day date = Day.from(LocalDate.ofEpochDay(t / Day.SECONDS_PER_DAY));
						Price price = new Price(date, new BigDecimal(String.valueOf(v)));
						if (isValidPrice(price)) {
							prices.add(price);
						}
					}
				}
				log.info("Loaded {} values for {}", prices.size(), getSecurity().getName());
			} else {
				log.error("No values found for {}", getSecurity().getName());
			}
		} else {
			log.error("Security not found: {}", getSecurity().getName());
		}
		setPrices(prices, true);
	}

	private boolean isValidPrice(Price price) {
		return price.getValue().signum() == 1 && price.getValue().compareTo(MAX_VALIDE_PRICE) < 0;
	}

	@SuppressWarnings("unchecked")
	private void readJsonCurrent(Reader reader) throws IOException, JsonParseException, JsonMappingException {
		List<IPrice> prices = new ArrayList<>();

		ObjectMapper mapper = new ObjectMapper();
		TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};
		Map<String,Object> jsonMap = mapper.readValue(reader, typeRef);

		jsonMap = (Map<String, Object>) jsonMap.get("quoteResponse");
		List<Object> results = (List<Object>) jsonMap.get("result");
		if (results != null) {
			Map<String, Object> result = (Map<String, Object>) results.get(0);

			double price = (double) result.get("regularMarketPrice");
			int time = (int) result.get("regularMarketTime");

			Day date = Day.from(LocalDate.ofEpochDay(time / Day.SECONDS_PER_DAY));
			BigDecimal value = new BigDecimal(String.valueOf(price));
			Price price2 = new Price(date, value);
			prices.add(price2);
		} else {
			log.error("Security not found: {}", getSecurity().getName());
		}

		setPrices(prices, true);
	}

}
