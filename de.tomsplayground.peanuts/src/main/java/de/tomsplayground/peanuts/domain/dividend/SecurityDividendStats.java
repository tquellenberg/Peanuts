package de.tomsplayground.peanuts.domain.dividend;

import java.math.BigDecimal;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.currenncy.Currencies;
import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;
import de.tomsplayground.peanuts.domain.currenncy.ExchangeRates;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

/**
 * FIXME: Join with {@link DividendStats}
 * FIXME: central calculation of YoC
 */
public final class SecurityDividendStats {

	private final ImmutableList<Dividend> dividends;

	public SecurityDividendStats(Security security) {
		dividends = security.getDividends();
	}
	
	// Shown in inventory table
	public BigDecimal getFutureDividendSum(BigDecimal quantity, ExchangeRates exchangeRates) {
		Day from = Day.today();
		Day to = from.addYear(1);
		return dividends.stream()
			.filter(d-> d.getPayDate().after(from) && d.getPayDate().beforeOrEquals(to))
			.map(d -> sumInDefaultCurrency(d, quantity, exchangeRates))
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
	
	// Shown in inventory table
	public BigDecimal getYoC(BigDecimal avgPricePerShare, ExchangeRates exchangeRates) {
		BigDecimal dividendPerShare = getFutureDividendSum(BigDecimal.ONE, exchangeRates);
		if (dividendPerShare.signum() == 0) {
			return BigDecimal.ZERO;
		}
		if (avgPricePerShare.signum() == 0) {
			return BigDecimal.ZERO;
		}
		return dividendPerShare.divide(avgPricePerShare, PeanutsUtil.MC);
	}

	private BigDecimal sumInDefaultCurrency(Dividend d, BigDecimal quantity, ExchangeRates exchangeRates) {
		BigDecimal amount = quantity.multiply(d.getAmountPerShare());

		CurrencyConverter converter = exchangeRates
				.createCurrencyConverter(d.getCurrency(), Currencies.getInstance().getDefaultCurrency());
		amount = converter.convert(amount, d.getPayDate());

		return amount;
	}
	
}
