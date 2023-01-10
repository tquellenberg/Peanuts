package de.tomsplayground.peanuts.scraping;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyXmlSerializer;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPather;
import org.htmlcleaner.XPatherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tomsplayground.peanuts.app.marketscreener.MarketScreener;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.process.PriceBuilder;
import de.tomsplayground.peanuts.util.Day;

public class Scraping {

	private final static Logger log = LoggerFactory.getLogger(Scraping.class);

	public static final String SCRAPING_XPATH = "scaping.close";

	public static final String SCRAPING_URL = "scaping.url";

	private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	private static CloseableHttpClient httpClient;

	private static HttpClientContext context;

	private static boolean isInitialized = false;

	private final String securityName;
	private final String scrapingUrl;
	private final String xpath;

	private String result = "";

	public static void main(String[] args) {
		Scraping scraping = new Scraping("Silber", "https://www.finanzen.net/rohstoffe/silberpreis", "//div[@class='snapshot__values-second']/div[1]/span[1]/text()");
		Price price = scraping.execute();
		System.out.println(price);
	}
	
	private static void init() {
		if (! isInitialized) {
			RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(StandardCookieSpec.RELAXED).build();
			context = HttpClientContext.create();
			httpClient = HttpClients.custom().setDefaultRequestConfig(globalConfig).build();
			isInitialized = true;
		}
	}

	public Scraping(Security security) {
		this.securityName = security.getName();
		this.scrapingUrl = security.getConfigurationValue(SCRAPING_URL);
		this.xpath = security.getConfigurationValue(SCRAPING_XPATH);
	}
	
	public Scraping(String securityName, String scrapingUrl, String xpath) {
		this.securityName = securityName;
		this.scrapingUrl = scrapingUrl;
		this.xpath = xpath;
	}

	public String getResult() {
		return result;
	}

	private String get(String url) throws IOException {
		HttpGet httpGet = new HttpGet(url);
		httpGet.setConfig(RequestConfig.custom()
			.setConnectionRequestTimeout(Timeout.ofSeconds(20)).build());
		httpGet.addHeader(HttpHeaders.USER_AGENT, MarketScreener.USER_AGENT);
		httpGet.addHeader(HttpHeaders.ACCEPT, "*/*");
		try {
			return httpClient.execute(httpGet, context, response -> {
				return EntityUtils.toString(response.getEntity());
			});
		} catch (IOException e) {
			log.error(e.getMessage()+ " " + securityName);
			return "";
		}
	}

	public Price execute() {
		init();
		Price price = null;
		if (StringUtils.isBlank(scrapingUrl)) {
			return price;
		}
		StringBuilder resultStr = new StringBuilder();
		try {
			HtmlCleaner htmlCleaner = new HtmlCleaner();
			TagNode tagNode = htmlCleaner.clean(get(scrapingUrl));

			PrettyXmlSerializer xmlSerializer = new PrettyXmlSerializer(htmlCleaner.getProperties());
			String string = xmlSerializer.getAsString(tagNode);

			PriceBuilder priceBuilder = new PriceBuilder();

			if (StringUtils.isNotEmpty(xpath)) {
				XPather xPather = new XPather(xpath);
				Object[] result = xPather.evaluateAgainstNode(tagNode);
				for (Object object : result) {
					resultStr.append('>').append(object).append('\n');
				}
				if (result.length > 0) {
					String value = result[0].toString().trim();
					priceBuilder.setClose(scapBigDecimal(value));
				}
			}

			price = priceBuilder.build();
			if (price.getValue().signum() == 0) {
				log.warn("Scrapping "+securityName + " no price found.");
				price = null;
			}
			resultStr.append("Price: ").append(price).append('\n');
			resultStr.append('\n').append(string);
		} catch (RuntimeException e) {
			log.error("Scrapping "+securityName, e);
			resultStr.append(e.getMessage());
		} catch (IOException e) {
			log.error("Scrapping "+securityName, e);
			resultStr.append(e.getMessage());
		} catch (XPatherException e) {
			log.error("Scrapping "+securityName, e);
			resultStr.append(e.getMessage());
		}
		result = resultStr.toString();
		return price;
	}

	private static String removeTrailingText(String value) {
		int i = StringUtils.indexOfAnyBut(value, "0123456789,.");
		if (i != -1) {
			value = value.substring(0, i);
		}
		return value;
	}

	public static Day scrapDay(String value) {
		value = removeTrailingText(value);
		Day date = Day.from(LocalDate.parse(value, dateFormatter));
		return date;
	}

	public static BigDecimal scapBigDecimal(String value) {
		value = removeTrailingText(value);
		if (value.indexOf(',') != -1 && value.indexOf('.') != -1) {
			// ',' and '.' => convert
			if (StringUtils.lastIndexOf(value, ',') > StringUtils.lastIndexOf(value, '.')) {
				value = StringUtils.remove(value, '.');
				value = value.replace(',', '.');
			} else {
				value = StringUtils.remove(value, ',');
			}
		} else if (value.indexOf(',') != -1) {
			// only ',' => convert to '.'
			value = value.replace(',', '.');
		}
		return new BigDecimal(value);
	}
}
