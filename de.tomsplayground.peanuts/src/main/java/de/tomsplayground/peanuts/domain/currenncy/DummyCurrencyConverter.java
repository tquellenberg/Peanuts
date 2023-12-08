package de.tomsplayground.peanuts.domain.currenncy;

import java.math.BigDecimal;
import java.util.Currency;

import de.tomsplayground.peanuts.util.Day;

public class DummyCurrencyConverter extends CurrencyConverter {

	public DummyCurrencyConverter(Currency c1) {
		super(null, c1, c1);
	}

	@Override
	public BigDecimal convert(BigDecimal value, Day day) {
		return value;
	}

	@Override
	public CurrencyConverter getInvertedCurrencyConverter() {
		return this;
	}

}
