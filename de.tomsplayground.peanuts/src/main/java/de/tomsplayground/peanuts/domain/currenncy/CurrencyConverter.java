package de.tomsplayground.peanuts.domain.currenncy;

import java.math.BigDecimal;
import java.util.Currency;

import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.util.PeanutsUtil;
import de.tomsplayground.util.Day;

public class CurrencyConverter {

	private final IPriceProvider priceProvider;
	private final Currency from;
	private final Currency to;
	private final boolean invertedPriceProvide;

	public CurrencyConverter(IPriceProvider priceProvider, Currency from, Currency to, boolean invertedPriceProvide) {
		this.priceProvider = priceProvider;
		this.from = from;
		this.to = to;
		this.invertedPriceProvide = invertedPriceProvide;
	}

	public CurrencyConverter(IPriceProvider priceProvider, Currency c1, Currency c2) {
		this(priceProvider, c1, c2, false);
	}

	public BigDecimal getRatio(Day day) {
		return convert(BigDecimal.ONE, day);
	}

	public BigDecimal convert(BigDecimal value, Day day) {
		return convert(value, day, invertedPriceProvide);
	}

	public CurrencyConverter getInvertedCurrencyConverter() {
		return new CurrencyConverter(priceProvider, to, from, !invertedPriceProvide);
	}

	private BigDecimal convert(BigDecimal value, Day day, boolean inverse) {
		if (from.equals(to)) {
			return value;
		}
		BigDecimal rate = priceProvider.getPrice(day).getClose();
		if (rate.signum() == 0) {
			throw new RuntimeException("Rate must not be zero. Date:"+day);
		}
		if (inverse) {
			return value.divide(rate, PeanutsUtil.MC);
		} else {
			return value.multiply(rate, PeanutsUtil.MC);
		}
	}

	public Currency getFromCurrency() {
		return from;
	}

	public Currency getToCurrency() {
		return to;
	}
}
