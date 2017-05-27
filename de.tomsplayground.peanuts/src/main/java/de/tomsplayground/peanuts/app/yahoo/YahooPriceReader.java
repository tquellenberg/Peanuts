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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
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
		HISTORICAL
	}

	private final DateFormat dateFormat2 = new SimpleDateFormat("MM/dd/yyyy");
	private final CSVReader csvReader;
	private final Type type;

	private static final CloseableHttpClient httpclient = HttpClients.createDefault();

	public static YahooPriceReader forTicker(Security security, String ticker, Type type) throws IOException {
		String url;
		if (type == Type.CURRENT) {
			url = "http://download.finance.yahoo.com/d/quotes.csv?f=sl1d1t1c1ohgv&s=" +
				URLEncoder.encode(ticker, StandardCharsets.UTF_8.name());
		} else {
			Calendar today = Calendar.getInstance();
			url = "https://query1.finance.yahoo.com/v7/finance/download/{0}?period1=1493067539&period2={1}&interval=1d&events=history&crumb={2}";
			url = MessageFormat.format(url,
				URLEncoder.encode(ticker, StandardCharsets.UTF_8.name()),
				String.valueOf(today.getTime().getTime()),
				URLEncoder.encode("2.h2NmyZMyR", StandardCharsets.UTF_8.name()));
		}
		HttpGet httpGet = new HttpGet(url);
		httpGet.addHeader("User-Agent", USER_AGENT);
		httpGet.addHeader("Cookie:", "B=97q6ontcibveo&b=3&s=ah; expires=Thu, 24-May-2018 21:40:41 GMT; path=/; domain=.yahoo.com");
		httpGet.setConfig(RequestConfig.custom()
			.setSocketTimeout(1000*20)
			.setConnectTimeout(1000*10)
			.setConnectionRequestTimeout(1000*10).build());
		CloseableHttpResponse response1 = null;
		try {
			response1 = httpclient.execute(httpGet);
			HttpEntity entity1 = response1.getEntity();
			String str = EntityUtils.toString(entity1);
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
		if (type == Type.HISTORICAL) {
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
		if (type == Type.HISTORICAL) {
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
