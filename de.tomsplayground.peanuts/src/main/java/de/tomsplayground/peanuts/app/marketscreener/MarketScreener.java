package de.tomsplayground.peanuts.app.marketscreener;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPather;
import org.htmlcleaner.XPatherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tomsplayground.peanuts.domain.fundamental.FundamentalData;

public class MarketScreener {

	private final static Logger log = LoggerFactory.getLogger(MarketScreener.class);

	private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.12; rv:52.0) Gecko/20100101 Firefox/52.0";

	private final RequestConfig defaultRequestConfig = RequestConfig.custom()
        .setConnectTimeout(1000 * 60)
        .setSocketTimeout(1000 * 60)
        .setConnectionRequestTimeout(1000 * 60)
        .build();

	private final CloseableHttpClient httpclient = HttpClientBuilder.create()
		.setDefaultRequestConfig(defaultRequestConfig).build();

	public static void main(String[] args) {
		List<FundamentalData> scrapFinancials = new MarketScreener().scrapFinancials("https://www.marketscreener.com/quote/stock/KELLOGG-COMPANY-13226/financials/");
		for (FundamentalData fundamentalData : scrapFinancials) {
			System.out.println(fundamentalData);
		}
	}

	private String getPage(URI url) throws IOException {
		HttpGet httpGet = new HttpGet(url);
		httpGet.addHeader("User-Agent", USER_AGENT);
		CloseableHttpResponse response1 = null;
		try {
			response1 = httpclient.execute(httpGet);
			HttpEntity entity1 = response1.getEntity();
			return EntityUtils.toString(entity1);
		} catch (IOException e) {
			log.error("URL"+url, e);
			throw e;
		} finally {
			if (response1 != null) {
				response1.close();
			}
			httpGet.releaseConnection();
		}
	}

	public List<FundamentalData> scrapFinancials(String financialsUrl) {
		if (financialsUrl.startsWith("http://www.4-traders.com/")) {
			financialsUrl = StringUtils.replace(financialsUrl, "http://www.4-traders.com/", "https://www.marketscreener.com/");
		}
		List<FundamentalData> fundamentalDatas = new ArrayList<>();
		try {
			String html = getPage(new URL(financialsUrl).toURI());
			HtmlCleaner htmlCleaner = new HtmlCleaner();
			TagNode tagNode = htmlCleaner.clean(html);

			// Table "Annual Income Statement Data"
			XPather xPather = new XPather("//table[@class='BordCollapseYear2']");
			Object[] result = xPather.evaluateAgainstNode(tagNode);
			TagNode incomeTable = null;
			if (result.length > 1 && result[1] instanceof TagNode) {
				incomeTable = (TagNode) result[1];
			}

			boolean isPence = false;
			xPather = new XPather("tbody/tr[12]/td[1]/div[2]/text()");
			result = xPather.evaluateAgainstNode(incomeTable);
			if (result.length > 0) {
				String currency = result[0].toString();
				if (StringUtils.contains(currency, "XXX")) {
					isPence = true;
				}
			}

			for (int i = 6; i <= 10; i++) {
				FundamentalData fundamentalData = new FundamentalData();

				// Year
				xPather = new XPather("tbody/tr[1]/td[" + i + "]/text()");
				result = xPather.evaluateAgainstNode(incomeTable);
				if (result.length == 0) {
					// Okay
					continue;
				}
				String yearStr = result[0].toString().strip();
				try {
					int year = Integer.parseInt(yearStr);
					fundamentalData.setYear(year);
				} catch (NumberFormatException e) {
					// Okay
					log.info("NumberFormatException: "+e.getMessage() + " '"+yearStr+"' "+financialsUrl);
					continue;
				}

				// EPS
				xPather = new XPather("tbody/tr[9]/td[" + i + "]/text()");
				result = xPather.evaluateAgainstNode(incomeTable);
				String cellText = result[0].toString();
				try {
					BigDecimal eps = parseNumber(cellText);
					if (isPence) {
						eps = eps.divide(new BigDecimal(100));
					}
					fundamentalData.setEarningsPerShare(eps);
				} catch (NumberFormatException e) {
					log.info("NumberFormatException: "+e.getMessage() + " '"+cellText+"' "+financialsUrl);
					// Okay
					continue;
				}

				// Dividend
				xPather = new XPather("tbody/tr[10]/td[" + i + "]/text()");
				result = xPather.evaluateAgainstNode(incomeTable);
				cellText = result[0].toString();
				try {
					BigDecimal dividend = parseNumber(cellText);
					if (isPence) {
						dividend = dividend.divide(new BigDecimal(100));
					}
					fundamentalData.setDividende(dividend);
				} catch (NumberFormatException e) {
					log.info("NumberFormatException: "+e.getMessage() + " '"+cellText+"' "+financialsUrl);
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
		r = StringUtils.remove(r, "<b>");
		r = StringUtils.remove(r, "<\\b>");
		return new BigDecimal(StringUtils.remove(r.replace(',', '.'), ' '));
	}

}
