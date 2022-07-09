package de.tomsplayground.peanuts.domain.calendar;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import de.tomsplayground.peanuts.app.yahoo.YahooCalendarEntry;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.util.Day;

public class CalendarUpdateJob {

	private YahooCalendarEntry yahooCalendarEntry;

	public void setApiKey(String apiKey) {
		yahooCalendarEntry = new YahooCalendarEntry(apiKey);
	}

	public boolean isEnabled(Security security) {
		String symbol = security.getConfigurationValue(Security.CONFIG_KEY_YAHOO_SYMBOL);
		return StringUtils.isNotBlank(symbol);
	}
	
	public Optional<SecurityCalendarEntry> findNewCalendarEntry(Security security, List<SecurityCalendarEntry> securityEntries) {
		// Future entry already exists
		for (SecurityCalendarEntry existingEntry : securityEntries) {
			if (existingEntry.getDay().after(Day.today())) {
				return Optional.empty();
			}
		}
		
		String symbol = security.getConfigurationValue(Security.CONFIG_KEY_YAHOO_SYMBOL);
		List<CalendarEntry> newEntries = yahooCalendarEntry.readUrl(symbol);
		if (newEntries.isEmpty()) {
			
		}
		for (CalendarEntry calendarEntry : newEntries) {
			if (calendarEntry.getDay().after(Day.today())) {
				return Optional.of(new SecurityCalendarEntry(security, calendarEntry.getDay(), calendarEntry.getName()));
			}
		}

		return Optional.empty();
	}
}
