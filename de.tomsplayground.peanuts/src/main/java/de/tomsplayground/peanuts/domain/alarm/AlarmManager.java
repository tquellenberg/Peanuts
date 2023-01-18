package de.tomsplayground.peanuts.domain.alarm;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.IPriceProviderFactory;
import de.tomsplayground.peanuts.util.Day;

public class AlarmManager {

	/**
	 *
	 * @return all SecurityAlarm which changed to 'triggered'.
	 */
	public List<SecurityAlarm> checkAlarms(ImmutableList<SecurityAlarm> securityAlarms, IPriceProviderFactory priceProviderFactory) {
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
