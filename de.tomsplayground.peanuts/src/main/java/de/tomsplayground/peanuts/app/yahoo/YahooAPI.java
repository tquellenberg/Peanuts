package de.tomsplayground.peanuts.app.yahoo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import de.tomsplayground.peanuts.util.Day;

public class YahooAPI {

	private final static Logger log = LoggerFactory.getLogger(YahooAPI.class);

	private final static CloseableHttpClient httpClient = HttpClients.createDefault();

	public static class YahooData {
		private final List<DebtEquityValue> debtEquityValue;
		private final MarketCap marketCap;

		public YahooData(List<DebtEquityValue> debtEquityValue, MarketCap marketCap) {
			this.debtEquityValue = debtEquityValue;
			this.marketCap = marketCap;
		}
		public List<DebtEquityValue> getDebtEquityValue() {
			return debtEquityValue;
		}
		public MarketCap getMarketCap() {
			return marketCap;
		}
		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
		}
	}

	public static class DebtEquityValue {
		private Day endDate;
		private long longTermDebt;
		private long totalStockholderEquity;

		public Day getDay() {
			return endDate;
		}

		public int getYear() {
			return endDate.addDays(-14).year;
		}

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
		}
		public double getValue() {
			return ((double)longTermDebt) / ((double) totalStockholderEquity);
		}
	}

	private static final String GET_FINANCIALS_URL = "https://apidojo-yahoo-finance-v1.p.rapidapi.com/stock/v2/get-financials?symbol={0}";
	private static final String SEARCH_URL = "https://apidojo-yahoo-finance-v1.p.rapidapi.com/auto-complete?q={0}";
	
	private static final String API_HOST = "apidojo-yahoo-finance-v1.p.rapidapi.com";

	private final String apiKey;

	public static void main(String[] args) {
		YahooAPI yahooAPI = new YahooAPI("");
//		yahooAPI.search("tesla");
		
		YahooData yahooData = yahooAPI.getYahooData("7974.T");		
		System.out.println(yahooData);
		List<DebtEquityValue> values = yahooData.debtEquityValue;
		for (DebtEquityValue debtEquityValue : values) {
			System.out.println(debtEquityValue + "   " + debtEquityValue.getValue()+ " " + debtEquityValue.getYear());
		}
	}

	public YahooAPI(String apiKey) {
		this.apiKey = apiKey;
	}

	public Map<String, Object> readUrl(String symbol, String urlTemplate) {
		String url = MessageFormat.format(urlTemplate, symbol);
		HttpGet httpGet = new HttpGet(url);
		httpGet.addHeader("x-rapidapi-host", API_HOST);
		httpGet.addHeader("x-rapidapi-key", apiKey);
		CloseableHttpResponse response1 = null;
		try {
			response1 = httpClient.execute(httpGet);
			HttpEntity entity1 = response1.getEntity();
			if (response1.getStatusLine().getStatusCode() != 200) {
				log.error(response1.getStatusLine().toString() + " "+url);
				for (Header h : response1.getAllHeaders()) {
					log.info(""+h);
				}
				log.info(EntityUtils.toString(entity1));
			} else {
				return jsonToMap(EntityUtils.toString(entity1));
			}
		} catch (IOException e) {
			log.error("URL "+url + " - " + e.getMessage());
			return null;
		} finally {
			if (response1 != null) {
				try {
					response1.close();
				} catch (IOException e) {
				}
			}
			httpGet.releaseConnection();
		}
		return null;
	}

	private MarketCap parseJsonDataForMarketCap(Map<String, Object> jsonMap) {
		Map<?, ?> summaryDetail = (Map<?, ?>) jsonMap.get("summaryDetail");
		if (summaryDetail == null) {
			return null;
		}
		String currency = (String) summaryDetail.get("currency");
		Map<?, ?> marketCapMap = (Map<?, ?>) summaryDetail.get("marketCap");
		BigDecimal marketCap = new BigDecimal(((Number)marketCapMap.get("raw")).longValue());
		return new MarketCap(marketCap, currency);
	}

	private List<DebtEquityValue> parseJsonDataForDebtEquity(Map<String, Object> jsonMap) {
		List<DebtEquityValue> result = new ArrayList<>();
		result.addAll(getDebtEquityData(jsonMap, "balanceSheetHistory"));
		List<DebtEquityValue> quarterly = getDebtEquityData(jsonMap, "balanceSheetHistoryQuarterly");
		if (! quarterly.isEmpty()) {
			quarterly.sort((a,b) -> b.endDate.compareTo(a.endDate));
			result.add(quarterly.get(0));
		}
		result.sort((a,b) -> a.endDate.compareTo(b.endDate));
		return result;
	}

	private Map<String, Object> jsonToMap(String json) throws JsonProcessingException, JsonMappingException {
		ObjectMapper mapper = new ObjectMapper();
		TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};
		Map<String,Object> jsonMap = mapper.readValue(json, typeRef);
		return jsonMap;
	}

	private List<DebtEquityValue> getDebtEquityData(Map<String,Object> jsonMap, String type) {
		List<DebtEquityValue> result = new ArrayList<>();
		Map data = (Map) jsonMap.get(type);
		if (data == null) {
			log.warn("No data of '{}' in JSON map", type);
			return result;
		}
		List<Map<String, Map>> jsonArray = (List<Map<String, Map>>) data.get("balanceSheetStatements");
		if (jsonArray == null) {
			log.warn("No data of 'balanceSheetStatements' in '{}' JSON map", type);
			return result;
		}
		for (int i=0; i< jsonArray.size(); i++) {
			Map jsonObject = jsonArray.get(i);
			if (jsonObject.containsKey("endDate") && jsonObject.containsKey("longTermDebt") && jsonObject.containsKey("totalStockholderEquity")) {
				DebtEquityValue debtEquityValue = new DebtEquityValue();
				debtEquityValue.endDate = Day.from(LocalDate.ofEpochDay(((Number)((Map)jsonObject.get("endDate")).get("raw")).longValue() / Day.SECONDS_PER_DAY));
				debtEquityValue.longTermDebt = ((Number)((Map)jsonObject.get("longTermDebt")).get("raw")).longValue();
				debtEquityValue.totalStockholderEquity = ((Number)((Map)jsonObject.get("totalStockholderEquity")).get("raw")).longValue();
				result.add(debtEquityValue);
			}
		}
		return result;
	}
	
	public YahooData getYahooData(String symbol) {
		Map<String, Object> jsonMap = readUrl(symbol, GET_FINANCIALS_URL);
		if (jsonMap == null) {
			return null;
		}
		
		List<DebtEquityValue> debtEquity = parseJsonDataForDebtEquity(jsonMap);
		MarketCap marketCap = parseJsonDataForMarketCap(jsonMap);
		return new YahooData(debtEquity, marketCap);		
	}
	
	public List<YahooSecurity> search(String query) {
		List<YahooSecurity> result = Lists.newArrayList();
		String queryEnc = "";
		try {
			queryEnc = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
		}
		Map<String, Object> resultJson = readUrl(queryEnc, SEARCH_URL);
		if (resultJson != null) {
			List<Map<String, String>> quotes = (List<Map<String, String>>) resultJson.get("quotes");
			if (quotes != null) {
				for (Map<String, String> quote : quotes) {
					System.out.println(quote);
					String symbol = quote.get("symbol");
					String name = quote.get("shortname");
					String exchange = quote.get("exchDisp");
					
					result.add(new YahooSecurity(symbol, name, exchange));
				}
			}
		}
		return result;
	}

}
