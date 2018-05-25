package de.tomsplayground.peanuts.app.yahoo;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
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
import de.tomsplayground.util.Day;

public class YahooPriceReader extends PriceProvider {

	private final static Logger log = LoggerFactory.getLogger(YahooPriceReader.class);

	private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.12; rv:52.0) Gecko/20100101 Firefox/52.0";

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
			RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build();
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
		log.info("URL: "+url);
		HttpGet httpGet = new HttpGet(url);
		httpGet.addHeader("User-Agent", USER_AGENT);
		httpGet.setConfig(RequestConfig.custom()
			.setCookieSpec(CookieSpecs.STANDARD)
			.setSocketTimeout(1000*20)
			.setConnectTimeout(1000*10)
			.setConnectionRequestTimeout(1000*10).build());
		CloseableHttpResponse response1 = null;
		try {
			response1 = httpClient.execute(httpGet, context);
			HttpEntity entity1 = response1.getEntity();
			if (response1.getStatusLine().getStatusCode() != 200) {
				log.error(response1.getStatusLine().toString() + " "+security.getName());
				return new YahooPriceReader(security, null, type);
			} else {
				String str = EntityUtils.toString(entity1);
				return new YahooPriceReader(security, new StringReader(str), type);
			}
		} catch (IOException e) {
			log.error("URL "+url + " - " + e.getMessage());
			return null;
		} finally {
			if (response1 != null) {
				response1.close();
			}
			httpGet.releaseConnection();
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
						Day date = Day.fromDate(new Date(t*1000L));
						prices.add(new Price(date, new BigDecimal(String.valueOf(v))));
					}
				}
				log.info("Loaded {} values for {}", prices.size(), getSecurity().getTicker());
			} else {
				log.error("No values found for {}", getSecurity().getName());
			}
		} else {
			log.error("Security not found: {}", getSecurity().getName());
		}
		setPrices(prices, true);
	}

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

			Day date = Day.fromDate(new Date(time*1000L));
			BigDecimal value = new BigDecimal(String.valueOf(price));
			Price price2 = new Price(date, value);
			prices.add(price2);
		} else {
			log.error("Security not found: {}", getSecurity().getName());
		}

		setPrices(prices, true);
	}

	@Override
	public String getName() {
		return "Yahoo";
	}

}
