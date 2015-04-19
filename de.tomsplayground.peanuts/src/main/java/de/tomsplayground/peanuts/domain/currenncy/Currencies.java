package de.tomsplayground.peanuts.domain.currenncy;

import java.util.Currency;

import com.google.common.collect.ImmutableList;

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

	public ImmutableList<Currency> getMainCurrencies() {
		return MAIN_CURRENCIES;
	}

}
