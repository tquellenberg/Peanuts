package de.tomsplayground.peanuts.domain.alarm;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.IPriceProviderFactory;
import de.tomsplayground.util.Day;

public class AlarmManager {

	public void checkAlarms(ImmutableList<SecurityAlarm> securityAlarms, IPriceProviderFactory priceProviderFactory) {
		securityAlarms.forEach(a->
			checkAlarm(a, priceProviderFactory.getPriceProvider(a.getSecurity())));
	}

	public void checkAlarm(SecurityAlarm securityAlarm, IPriceProvider priceProvider) {
		if (priceProvider == null) {
			return;
		}
		Day day = securityAlarm.getStartDay();
		Day today = new Day();
		boolean triggerd = securityAlarm.isTriggered();
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
	}
}
