package de.tomsplayground.peanuts.app.marketscreener;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPather;
import org.htmlcleaner.XPatherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.fundamental.FundamentalData;

public class MarketScreener {

	private final static Logger log = LoggerFactory.getLogger(MarketScreener.class);

	public static final String CONFIG_KEY_URL = "fourTrasdersUrl";

	public static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36";

	private final static RequestConfig defaultRequestConfig = RequestConfig.custom()
		.setConnectionRequestTimeout(Timeout.ofSeconds(30))
        .build();
	
	private final static CloseableHttpClient httpclient = HttpClientBuilder.create()
		.setDefaultRequestConfig(defaultRequestConfig)
		.setRedirectStrategy(new DefaultRedirectStrategy())
		.build();

	public static void main(String[] args) {
		Result result = new MarketScreener().scrapFinancials("https://www.marketscreener.com/quote/stock/THE-GOLDMAN-SACHS-GROUP-I-12831/finances/");
		for (FundamentalData fundamentalData : result.datas) {
			System.out.println(fundamentalData);
		}
	}
	
	private record HttpResponse(String content, String newUrl, boolean moved) {};

	private HttpResponse getPage(URI url) throws IOException {
		HttpGet httpGet = new HttpGet(url);
		httpGet.addHeader("User-Agent", USER_AGENT);
		try {
			 HttpResponse response = executeRequest(httpGet);
			 if (response.moved) {
				 String newUrl = response.newUrl;
				 httpGet.setUri(URI.create(newUrl));
				 response = executeRequest(httpGet);
				 if (! response.moved) {
					 response = new HttpResponse(response.content, newUrl, true);
				 }
			 }
			 return response;
		} catch (IOException e) {
			log.error("URL"+url, e);
			throw e;
		}
	}

	private HttpResponse executeRequest(HttpGet httpGet) throws IOException {
		return httpclient.execute(httpGet, response -> {
			int code = response.getCode();
			String content = EntityUtils.toString(response.getEntity());
			if (code == 302) {
				String newUrl = "";
				Header header = response.getHeader("Location");
				if (header != null) {
					// Look into header
					newUrl = response.getHeader("Location").getValue();
				} else {
					// Parse content
					Pattern pattern = Pattern.compile("url='(.*)'");
					Matcher matcher = pattern.matcher(content);
					if (matcher.find()) {
						newUrl = "https://www.marketscreener.com"+matcher.group(1);
					}
				}
				log.info("Moved: {}", newUrl);
				return new HttpResponse("", newUrl, true);
			}
			return new HttpResponse(content, "", false);
		});
	}

	public List<FundamentalData> scrapFinancials(Security security) {
		String financialsUrl = security.getConfigurationValue(MarketScreener.CONFIG_KEY_URL);
		String fixedUrl = fixMarketscreenerUrl(financialsUrl);
		if (! financialsUrl.equals(fixedUrl)) {
			security.putConfigurationValue(MarketScreener.CONFIG_KEY_URL, fixedUrl);
			financialsUrl = fixedUrl;
		}
		if (StringUtils.isNotBlank(financialsUrl)) {
			Result result = scrapFinancials(financialsUrl);
			if (!result.datas.isEmpty() && result.response.moved && StringUtils.isNoneBlank(result.response.newUrl)) {
				security.putConfigurationValue(MarketScreener.CONFIG_KEY_URL, result.response.newUrl);
			}
			return result.datas;
		} else {
			return new ArrayList<>();
		}
	}

	private String fixMarketscreenerUrl(String financialsUrl) {
		if (financialsUrl.startsWith("http://www.4-traders.com/")) {
			financialsUrl = StringUtils.replace(financialsUrl, "http://www.4-traders.com/", "https://www.marketscreener.com/");
		}
		if (financialsUrl.endsWith("/financials/")) {
			financialsUrl = StringUtils.replace(financialsUrl, "/financials/", "/finances/");
		}
		if (! financialsUrl.contains("/quote/stock/")) {
			financialsUrl = StringUtils.replace(financialsUrl, "https://www.marketscreener.com/", "https://www.marketscreener.com/quote/stock/");
		}
		return financialsUrl;
	}
	
	private record Result(List<FundamentalData> datas, HttpResponse response) {};
	
	private Result scrapFinancials(String financialsUrl) {
		log.info("URL: {}", financialsUrl);
		List<FundamentalData> fundamentalDatas = new ArrayList<>();
		HttpResponse response = null;
		try {
			response = getPage(new URI(financialsUrl));
			String html = response.content;

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
			return new Result(fundamentalDatas, response);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XPatherException e) {
			e.printStackTrace();
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		}
		return new Result(new ArrayList<>(), response);
	}

	private BigDecimal parseNumber(String r) {
		r = r.replaceAll("\\s","");
		r = StringUtils.remove(r, "<b>");
		r = StringUtils.remove(r, "<\\b>");
		return new BigDecimal(r.replace(',', '.'));
	}

}
