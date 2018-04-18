package de.tomsplayground.peanuts.app.yahoo;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;
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

	private final DateFormat dateFormat2 = new SimpleDateFormat("MM/dd/yyyy");
	private final CSVReader csvReader;
	private final Type type;

	private static CloseableHttpClient httpClient;

	private static Crumb crumb;
	private static HttpClientContext context;

	private static void init() {
		RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build();
		CookieStore cookieStore = new BasicCookieStore();
		context = HttpClientContext.create();
		context.setCookieStore(cookieStore);
		httpClient = HttpClients.custom().setDefaultRequestConfig(globalConfig).setDefaultCookieStore(cookieStore).build();
	}

	private static Crumb loadCrump(String tickerSymbol) throws IOException {
		String url = MessageFormat.format("https://de.finance.yahoo.com/quote/{0}/history?p={0}", tickerSymbol); //$NON-NLS-1$
		log.info("Crumb-URL: "+url);
		HttpGet httpGet = new HttpGet(url);
		try {
			httpGet.addHeader("User-Agent", USER_AGENT);
			httpGet.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			httpGet.addHeader("Accept-Language", "en-US,en;q=0.5");
			httpGet.setConfig(RequestConfig.custom()
				.setCookieSpec(CookieSpecs.STANDARD)
				.setSocketTimeout(1000*20)
				.setConnectTimeout(1000*10)
				.setConnectionRequestTimeout(1000*10).build());

			CloseableHttpResponse response1 = httpClient.execute(httpGet, context);
			if (response1.getStatusLine().getStatusCode() != 200) {
				log.error(response1.getStatusLine().toString());
			}
			HttpEntity entity1 = response1.getEntity();
			String body = EntityUtils.toString(entity1);
			log.info(body);

			String KEY = "\"CrumbStore\":{\"crumb\":\""; //$NON-NLS-1$

			int startIndex = body.indexOf(KEY);
			if (startIndex < 0) {
				throw new IOException("No Crumb Found");
			}

			int endIndex = body.indexOf('"', startIndex + KEY.length());
			if (endIndex < 0) {
				throw new IOException("No Crumb Found");
			}

			String crumb = body.substring(startIndex + KEY.length(), endIndex);
			log.error("crumb: {}", crumb);
			crumb = StringEscapeUtils.unescapeJava(crumb);

			return new Crumb(crumb, context.getCookieStore().getCookies());
		} finally {
			httpGet.releaseConnection();
		}
	}

	public static YahooPriceReader forTicker(Security security, String ticker, Type type) throws IOException {
		if (crumb == null) {
			init();
			crumb = loadCrump(ticker);
		}

		String url;
		if (type == Type.CURRENT) {
			url = "http://download.finance.yahoo.com/d/quotes.csv?f=sl1d1t1c1ohgv&s=" +
				URLEncoder.encode(ticker, StandardCharsets.UTF_8.name());
		} else {
			Calendar today = Calendar.getInstance();
			DateTime startDate;
			if (type == Type.HISTORICAL) {
				startDate = new DateTime(2000,1,1,0,0);
			} else {
				startDate = new DateTime().minusDays(14);
			}
			url = "https://query1.finance.yahoo.com/v7/finance/download/{0}?period1={1}&period2={2}&interval=1d&events=history&crumb={3}";
			url = MessageFormat.format(url,
				URLEncoder.encode(ticker, StandardCharsets.UTF_8.name()),
				String.valueOf(startDate.toDate().getTime() / 1000L),
				String.valueOf(today.getTime().getTime() / 1000L),
				URLEncoder.encode(crumb.getId(), StandardCharsets.UTF_8.name()));
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
			String str = EntityUtils.toString(entity1);
			if (response1.getStatusLine().getStatusCode() != 200) {
				log.error(response1.getStatusLine().toString() +" " +str);
			}
			return new YahooPriceReader(security, new StringReader(str), type);
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
		if (type == Type.HISTORICAL || type == Type.LAST_DAYS) {
			csvReader = new CSVReader(reader, ',', '"');
		} else {
			csvReader = new CSVReader(reader, ',', '"');
		}
		this.type = type;
		read();
	}

	private void read() throws IOException {
		String values[];
		List<IPrice> prices = new ArrayList<>();
		if (type == Type.HISTORICAL || type == Type.LAST_DAYS) {
			// Skip header
			csvReader.readNext();
			while ((values = csvReader.readNext()) != null) {
				if (StringUtils.isNotBlank(values[0])) {
					try {
						Day d = Day.fromString(values[0]);
						if (d.year < 3000) {
							BigDecimal open = getValue(values, 1);
							BigDecimal high = getValue(values, 2);
							BigDecimal low = getValue(values, 3);
							BigDecimal close = getValue(values, 4);
							Price price = new Price(d, open, close, high, low);
							if (price.getValue().compareTo(BigDecimal.ZERO) > 0) {
								prices.add(price);
							}
						}
					} catch (NumberFormatException e) {
						log.error("Value: " + Arrays.toString(values));
						throw e;
					} catch (IllegalArgumentException e) {
						log.error("Value: " + Arrays.toString(values));
						throw e;
					}
				}
			}
		} else {
			values = csvReader.readNext();
			if (values != null && values.length >= 7 && !values[2].equals("N/A")) {
				int startPos = 5;
				try {
					Day d = Day.fromDate(dateFormat2.parse(values[2]));
					if (d.year < 3000) {
						BigDecimal close = readDecimal(values[1]);
						BigDecimal open = readDecimal(values[startPos]);
						BigDecimal high = readDecimal(values[startPos+1]);
						BigDecimal low = readDecimal(values[startPos+2]);
						prices.add(new Price(d, open, close, high, low));
					}
				} catch (ParseException e) {
					log.error(e.getMessage()+ " Value: " + Arrays.toString(values), e);
				}
			} else {
				log.error("Invalid input: " + Arrays.toString(values));
			}
		}
		setPrices(prices, true);
		csvReader.close();
	}

	private BigDecimal getValue(String[] values, int col) {
		if (col >= values.length || StringUtils.isBlank(values[col])) {
			return null;
		}
		try {
			return new BigDecimal(values[col]);
		} catch (NumberFormatException e) {
			log.error("Invalid input: " + Arrays.toString(values));
			return null;
		}
	}

	private BigDecimal readDecimal(String value) {
		value = value.replace(',', '.');
		try  {
			return new BigDecimal(value);
		} catch (NumberFormatException e) {
			log.error("readDecimal: '" + value+"' "+e.getMessage());
			return null;
		}
	}

	@Override
	public String getName() {
		return "Yahoo";
	}

}
