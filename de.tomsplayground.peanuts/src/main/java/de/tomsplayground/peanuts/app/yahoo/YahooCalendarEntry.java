package de.tomsplayground.peanuts.app.yahoo;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.tomsplayground.peanuts.domain.calendar.CalendarEntry;
import de.tomsplayground.peanuts.util.Day;

public class YahooCalendarEntry {

	private final static Logger log = LoggerFactory.getLogger(YahooCalendarEntry.class);

	private final static CloseableHttpClient httpClient = HttpClients.createDefault();

	private static final String API_URL = "https://apidojo-yahoo-finance-v1.p.rapidapi.com/stock/v2/get-profile?symbol={0}";
	private static final String API_HOST = "apidojo-yahoo-finance-v1.p.rapidapi.com";

	private final String apiKey;

	public static void main(String[] args) {
		List<CalendarEntry> values = new YahooCalendarEntry("").readUrl("DIS");
		for (CalendarEntry debtEquityValue : values) {
			System.out.println(debtEquityValue + "   " + debtEquityValue.getDay()+ " " + debtEquityValue.getName());
		}
	}

	public YahooCalendarEntry(String apiKey) {
		this.apiKey = apiKey;
	}

	public List<CalendarEntry> readUrl(String symbol) {
		if (StringUtils.isBlank(symbol)) {
			return Collections.emptyList();
		}
		String url = MessageFormat.format(API_URL, symbol);
		HttpGet httpGet = new HttpGet(url);
		httpGet.addHeader("x-rapidapi-host", API_HOST);
		httpGet.addHeader("x-rapidapi-key", apiKey);
		CloseableHttpResponse response1 = null;
		try {
			response1 = httpClient.execute(httpGet);
			HttpEntity entity1 = response1.getEntity();
			if (response1.getStatusLine().getStatusCode() != 200) {
				log.error(response1.getStatusLine().toString() + " "+url);
			} else {
				return parseJsonData(EntityUtils.toString(entity1), symbol);
			}
		} catch (IOException e) {
			log.error("URL "+url + " - " + e.getMessage());
			return Collections.emptyList();
		} finally {
			if (response1 != null) {
				try {
					response1.close();
				} catch (IOException e) {
				}
			}
			httpGet.releaseConnection();
		}
		return Collections.emptyList();
	}

	@SuppressWarnings("unchecked")
	private List<CalendarEntry> parseJsonData(String json, String symbol) throws JsonParseException, JsonMappingException, IOException {
		List<CalendarEntry> result = new ArrayList<>();
		ObjectMapper mapper = new ObjectMapper();
		TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};
		Map<String,Object> jsonMap = mapper.readValue(json, typeRef);
		Map<String,Object> calendarEvents = (Map<String, Object>) jsonMap.get("calendarEvents");
		Map<String,Object> earnings = (Map<String, Object>) calendarEvents.get("earnings");

		List<Map<String,Object>> earningsDate = (List<Map<String, Object>>) earnings.get("earningsDate");
		if (! earningsDate.isEmpty()) {
			Map<String,Object> date = earningsDate.get(0);
			Day day = Day.from(LocalDate.ofEpochDay(((Number)date.get("raw")).longValue() / Day.SECONDS_PER_DAY));
			String name = "Earnings";
			result.add(new CalendarEntry(day, name));
		} else {
			log.info("No earning date in JSON for {}", symbol);
		}
		return result;
	}

}
