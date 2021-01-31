package de.tomsplayground.peanuts.domain.currenncy;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Security;

public class Currencies {

	private final static Currencies INSTANCE = new Currencies();

	private final static ImmutableList<Currency> MAIN_CURRENCIES = ImmutableList.of(
		Currency.getInstance("USD"),
		Currency.getInstance("EUR"),
		Currency.getInstance("GBP"),
		Currency.getInstance("CHF"),
		Currency.getInstance("JPY")
	);

	private final static Currency DEFAULT_CURRENCY = MAIN_CURRENCIES.get(1);

	private Currencies() {
	}

	public static Currencies getInstance() {
		return INSTANCE;
	}

	public Currency getDefaultCurrency() {
		return DEFAULT_CURRENCY;
	}

	public ImmutableList<Currency> getCurrenciesWithExchangeSecurity(AccountManager accountManager) {
		List<Currency> currencies = new ArrayList<>(MAIN_CURRENCIES);
		for (Security security : accountManager.getSecurities()) {
			Currency exchangeCurrency = security.getExchangeCurrency();
			if (exchangeCurrency != null) {
				if (! currencies.contains(exchangeCurrency)) {
					currencies.add(exchangeCurrency);
				}
			}
		}
		currencies.sort((a,b) -> a.getDisplayName().compareTo(b.getDisplayName()));
		return ImmutableList.copyOf(currencies);
	}

}
