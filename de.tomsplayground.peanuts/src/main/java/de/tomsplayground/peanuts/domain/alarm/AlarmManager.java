package de.tomsplayground.peanuts.domain.alarm;

import static de.tomsplayground.peanuts.domain.alarm.SecurityAlarm.Mode.YEAR_HIGH;
import static de.tomsplayground.peanuts.domain.alarm.SecurityAlarm.Mode.YEAR_LOW;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.IPriceProviderFactory;
import de.tomsplayground.peanuts.domain.process.IStockSplitProvider;
import de.tomsplayground.peanuts.domain.statistics.SecurityHighLow;
import de.tomsplayground.peanuts.domain.statistics.SecurityHighLow.HighLowEntry;
import de.tomsplayground.peanuts.util.Day;

public class AlarmManager {

	private final SecurityHighLow securityHighLow;
	private final IPriceProviderFactory priceProviderFactory;
	
	public AlarmManager(IPriceProviderFactory priceProviderFactory) {
		this.priceProviderFactory = priceProviderFactory;
		this.securityHighLow = new SecurityHighLow(priceProviderFactory);
	}
	
	public List<SecurityAlarm> checkHighLow(List<Security> securities, IStockSplitProvider stockSplitProvider) {
		List<SecurityAlarm> highLowAlarms = new ArrayList<>();
		List<HighLowEntry> allHighLow = securityHighLow.allHighLow(securities, stockSplitProvider);
		
		Day limit = Day.today().addDays(-14);
		for (HighLowEntry highLowEntry : allHighLow) {
			Day day = highLowEntry.high().getDay();
			if (day.after(limit)) {
				SecurityAlarm alarm = new SecurityAlarm(highLowEntry.security(), YEAR_HIGH, highLowEntry.high().getValue(), null);
				alarm.setTriggerDay(day);
				highLowAlarms.add(alarm);
			}
			
			day = highLowEntry.low().getDay();
			if (day.after(limit)) {
				SecurityAlarm alarm = new SecurityAlarm(highLowEntry.security(), YEAR_LOW, highLowEntry.low().getValue(), null);
				alarm.setTriggerDay(day);
				highLowAlarms.add(alarm);
			}
		}
		return highLowAlarms;
	}
	
	/**
	 *
	 * @return all SecurityAlarm which changed to 'triggered'.
	 */
	public List<SecurityAlarm> checkAlarms(ImmutableList<SecurityAlarm> securityAlarms) {
		return securityAlarms.stream()
			.filter(a-> checkAlarm(a, priceProviderFactory.getPriceProvider(a.getSecurity())))
			.collect(Collectors.toList());
	}

	/**
	 *
	 * @return true, when the state changes from not triggered to triggered.
	 */
	private boolean checkAlarm(SecurityAlarm securityAlarm, IPriceProvider priceProvider) {
		if (priceProvider == null) {
			return false;
		}
		Day day = securityAlarm.getStartDay();
		Day today = Day.today();
		boolean triggerd = securityAlarm.isTriggered();
		if (triggerd) {
			return false;
		}
		while (day.beforeOrEquals(today) && !triggerd) {
			IPrice price = priceProvider.getPrice(day);
			if (price != null) {
				triggerd = switch (securityAlarm.getMode()) {
					case PRICE_ABOVE -> price.getValue().compareTo(securityAlarm.getValue()) > 0;
					case PRICE_BELOW -> price.getValue().compareTo(securityAlarm.getValue()) < 1;
					default -> securityAlarm.isTriggered();
				};
				if (triggerd) {
					securityAlarm.trigger(day);
				}
			}
			day = day.addDays(1);
		}
		return triggerd;
	}
}
