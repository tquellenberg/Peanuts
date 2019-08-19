package de.tomsplayground.peanuts.domain.alarm;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.IPriceProviderFactory;
import de.tomsplayground.util.Day;

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
		Day today = new Day();
		boolean triggerd = securityAlarm.isTriggered();
		if (triggerd) {
			return false;
		}
		while (!day.after(today) && !triggerd) {
			IPrice price = priceProvider.getPrice(day);
			if (price != null) {
				switch (securityAlarm.getMode()) {
					case PRICE_ABOVE:
						triggerd = price.getClose().compareTo(securityAlarm.getValue()) > 0;
						break;
					case PRICE_BELOW:
						triggerd = price.getClose().compareTo(securityAlarm.getValue()) < 1;
						break;
				}
				if (triggerd) {
					securityAlarm.trigger(day);
				}
			}
			day = day.addDays(1);
		}
		return triggerd;
	}
}
