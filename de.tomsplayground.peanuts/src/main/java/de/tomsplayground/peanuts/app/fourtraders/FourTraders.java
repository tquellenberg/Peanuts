package de.tomsplayground.peanuts.app.fourtraders;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPather;
import org.htmlcleaner.XPatherException;

import com.google.common.collect.Lists;

import de.tomsplayground.peanuts.domain.fundamental.FundamentalData;

public class FourTraders {

	private static final String SEARCH_URL = "http://www.4-traders.com/indexbasegauche.php?lien=recherche&mots=ISIN&RewriteLast=zbat&type_recherche=0";

	private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.12; rv:52.0) Gecko/20100101 Firefox/52.0";

	private final CloseableHttpClient httpclient = HttpClients.createDefault();

	public static void main(String[] args) {
		new FourTraders().read("US0378331005");
	}

	public List<FundamentalData> read(String isin) {
		String financialsUrl = findCompanyBaseUrl(isin);
		return scrapFinancials(financialsUrl);
	}

	private String getPage(URI url) throws IOException {
		HttpGet httpGet = new HttpGet(url);
		httpGet.addHeader("User-Agent", USER_AGENT);
		CloseableHttpResponse response1 = httpclient.execute(httpGet);
		try {
			HttpEntity entity1 = response1.getEntity();
			return EntityUtils.toString(entity1);
		} finally {
			response1.close();
			httpGet.releaseConnection();
		}
	}

	public List<FundamentalData> scrapFinancials(String financialsUrl) {
		List<FundamentalData> fundamentalDatas = Lists.newArrayList();
		try {
			String html = getPage(new URL(financialsUrl).toURI());
			HtmlCleaner htmlCleaner = new HtmlCleaner();
			TagNode tagNode = htmlCleaner.clean(html);

			boolean isPence = false;
			XPather xPather = new XPather("//table[@class='BordCollapseYear']/tbody/tr[9]/td[1]/text()");
			Object[] result = xPather.evaluateAgainstNode(tagNode);
			if (result.length > 0) {
				String currency = StringUtils.trim(StringUtils.substringBetween(result[0].toString(), "(", ")"));
				if (StringUtils.equals(currency, "PNC") || StringUtils.equals(currency, "GBp")) {
					isPence = true;
				}
			}

			for (int i = 4; i < 8; i++) {
				FundamentalData fundamentalData = new FundamentalData();

				xPather = new XPather("//table[@class='BordCollapseYear']/tbody/tr[2]/td[" + i + "]/text()");
				result = xPather.evaluateAgainstNode(tagNode);
				if (result.length == 0) {
					// Okay
					continue;
				}
				try {
					int year = Integer.parseInt(result[0].toString());
					fundamentalData.setYear(year);
				} catch (NumberFormatException e) {
					// Okay
					continue;
				}

				xPather = new XPather("//table[@class='BordCollapseYear']/tbody/tr[9]/td[" + i + "]/text()");
				result = xPather.evaluateAgainstNode(tagNode);
				try {
					BigDecimal eps = parseNumber(result[0].toString());
					if (isPence) {
						eps = eps.divide(new BigDecimal(100));
					}
					fundamentalData.setEarningsPerShare(eps);
				} catch (NumberFormatException e) {
					// Okay
					continue;
				}

				xPather = new XPather("//table[@class='BordCollapseYear']/tbody/tr[10]/td[" + i + "]/text()");
				result = xPather.evaluateAgainstNode(tagNode);
				try {
					BigDecimal dividend = parseNumber(result[0].toString());
					if (isPence) {
						dividend = dividend.divide(new BigDecimal(100));
					}
					fundamentalData.setDividende(dividend);
				} catch (NumberFormatException e) {
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
		return new BigDecimal(StringUtils.remove(r.replace(',', '.'), ' '));
	}

	public String scrapFinancialsUrl(String isin) {
		String companyBaseUrl = findCompanyBaseUrl(isin);
		if (StringUtils.isBlank(companyBaseUrl)) {
			return StringUtils.EMPTY;
		}
		return "http://www.4-traders.com"+companyBaseUrl+"financials/";
	}

	private String findCompanyBaseUrl(String isin) {
		String url = StringUtils.replace(SEARCH_URL, "ISIN", isin);
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setInstanceFollowRedirects(false);
			connection.getInputStream();
			Map<String, List<String>> headerFields = connection.getHeaderFields();
			if (headerFields.containsKey("Location")) {
				return headerFields.get("Location").get(0);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IOUtils.close(connection);
		}
		return "";
	}

}
