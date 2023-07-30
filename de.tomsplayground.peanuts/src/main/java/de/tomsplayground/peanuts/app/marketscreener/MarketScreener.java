package de.tomsplayground.peanuts.app.marketscreener;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPather;
import org.htmlcleaner.XPatherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tomsplayground.peanuts.domain.fundamental.FundamentalData;

public class MarketScreener {

	private final static Logger log = LoggerFactory.getLogger(MarketScreener.class);

	public static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";

	private final RequestConfig defaultRequestConfig = RequestConfig.custom()
		.setConnectionRequestTimeout(Timeout.ofSeconds(30))
        .build();
	
	private final CloseableHttpClient httpclient = HttpClientBuilder.create()
		.setDefaultRequestConfig(defaultRequestConfig).build();

	public static void main(String[] args) {
		List<FundamentalData> scrapFinancials = new MarketScreener().scrapFinancials("https://www.marketscreener.com/quote/stock/DWS-GROUP-GMBH-CO-KGAA-42452445/finances/");
		for (FundamentalData fundamentalData : scrapFinancials) {
			System.out.println(fundamentalData);
		}
	}

	private String getPage(URI url) throws IOException {
		HttpGet httpGet = new HttpGet(url);
		httpGet.addHeader("User-Agent", USER_AGENT);
		try {
			return httpclient.execute(httpGet, response -> {
				return EntityUtils.toString(response.getEntity());
			});
		} catch (IOException e) {
			log.error("URL"+url, e);
			throw e;
		}
	}

	public List<FundamentalData> scrapFinancials(String financialsUrl) {
		if (financialsUrl.startsWith("http://www.4-traders.com/")) {
			financialsUrl = StringUtils.replace(financialsUrl, "http://www.4-traders.com/", "https://www.marketscreener.com/");
		}
		if (financialsUrl.endsWith("/financials/")) {
			financialsUrl = StringUtils.replace(financialsUrl, "/financials/", "/finances/");
		}
		if (! financialsUrl.contains("/quote/stock/")) {
			financialsUrl = StringUtils.replace(financialsUrl, "https://www.marketscreener.com/", "https://www.marketscreener.com/quote/stock/");
		}
		log.info("URL: {}", financialsUrl);
		List<FundamentalData> fundamentalDatas = new ArrayList<>();
		try {
			String html = getPage(new URI(financialsUrl));

//			FileUtils.writeStringToFile(new File("./test.html"), html, Charset.forName("UTF-8"));			
//			String html = FileUtils.readFileToString(new File("./test.html"), Charset.forName("UTF-8"));
			
			HtmlCleaner htmlCleaner = new HtmlCleaner();
			TagNode tagNode = htmlCleaner.clean(html);

			// Table "Annual Income Statement Data"
			XPather xPather = new XPather("//table[@id='iseTableA']");
			Object[] result = xPather.evaluateAgainstNode(tagNode);
			TagNode incomeTable = null;
			if (result.length == 0) {
				log.error("Income table not found in HTML.");
			} else if (result.length == 1 && result[0] instanceof TagNode resultNode) {
				incomeTable = resultNode;
			} else if (result.length > 1) {
				log.error("More than one table found in HTML. ({})", result.length);
			} else {
				log.error("Problem with income table. {}", result[0].getClass());
			}

			// Find rows
			int epsRow = -1;
			int dividendRow = -1;
			for (int j = 1; j <= 20; j++) {
				xPather = new XPather("tbody/tr["+j+"]/td[1]/text()");
				result = xPather.evaluateAgainstNode(incomeTable);
				if (result.length == 0) {
					// Okay
					continue;
				}
				String rowText = result[0].toString().strip();
				if (StringUtils.indexOf(rowText, "EPS") >= 0) {
					epsRow = j;
				}
				if (StringUtils.indexOf(rowText, "Dividend") >= 0) {
					dividendRow = j;
				}
			}
			if (epsRow == -1) {
				log.error("EPS row not found in table.");
			}
			if (dividendRow == -1) {
				log.error("Dividend row not found in table.");
			}
			
			for (int i = 2; i <= 15; i++) {
				FundamentalData fundamentalData = new FundamentalData();

				// Year
				xPather = new XPather("thead/tr[1]/th[" + i + "]/span[1]/text()");
				result = xPather.evaluateAgainstNode(incomeTable);
				if (result.length == 0) {
					// Okay
					continue;
				}
				String yearStr = result[0].toString().strip();
				try {
					int year = Integer.parseInt(yearStr);
					if (year <= 2000) {
						log.info("Wrong value for year {}", yearStr);
						continue;
					}
					fundamentalData.setYear(year);
				} catch (NumberFormatException e) {
					// Okay
					log.info("NumberFormatException: "+e.getMessage() + " '"+yearStr+"' "+financialsUrl);
					continue;
				}

				// EPS
				xPather = new XPather("tbody/tr[" + epsRow + "]/td[" + i + "]/text()");
				result = xPather.evaluateAgainstNode(incomeTable);
				if (result.length == 0) {
					log.error("EPS not found in {},{}", epsRow, i);
				}
				String cellText = result[0].toString();
				try {
					fundamentalData.setEarningsPerShare(parseNumber(cellText));
				} catch (NumberFormatException e) {
					log.info("EPS NumberFormatException: "+e.getMessage() + " '"+cellText+"' "+financialsUrl);
					// Okay
					continue;
				}

				// Dividend
				xPather = new XPather("tbody/tr[" + dividendRow + "]/td[" + i + "]/text()");
				result = xPather.evaluateAgainstNode(incomeTable);
				if (result.length == 0) {
					log.error("Dividend not found in {},{}", dividendRow, i);
				}
				cellText = result[0].toString();
				try {
					if (StringUtils.equals("-", cellText.trim())) {
						fundamentalData.setDividende(BigDecimal.ZERO);
					} else {
						fundamentalData.setDividende(parseNumber(cellText));
					}
				} catch (NumberFormatException e) {
					log.info("Dividend NumberFormatException: "+e.getMessage() + " '"+cellText+"' "+financialsUrl);
					// Okay
				}

				fundamentalDatas.add(fundamentalData);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XPatherException e) {
			e.printStackTrace();
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		}
		return fundamentalDatas;
	}

	private BigDecimal parseNumber(String r) {
		r = r.replaceAll("\\s","");
		r = StringUtils.remove(r, "<b>");
		r = StringUtils.remove(r, "<\\b>");
		return new BigDecimal(r.replace(',', '.'));
	}

}
