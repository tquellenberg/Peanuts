package de.tomsplayground.peanuts.domain.fundamental;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;
import de.tomsplayground.peanuts.domain.process.CurrencyAdjustedPriceProvider;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.util.Day;

public class CurrencyAjustedFundamentalData extends FundamentalData {

	CurrencyConverter currencyConverter;

	public CurrencyAjustedFundamentalData(FundamentalData fundamentalData, CurrencyConverter currencyConverter) {
		super(fundamentalData);
		this.currencyConverter = currencyConverter;
	}

	@Override
	public BigDecimal getDividende() {
		return currencyConverter.convert(super.getDividende(), new Day(super.getYear(), 11, 30));
	}

	@Override
	public BigDecimal getEarningsPerShare() {
		return currencyConverter.convert(super.getEarningsPerShare(), new Day(super.getYear(), 11, 30));
	}

	@Override
	public BigDecimal calculatePeRatio(IPriceProvider priceProvider) {
		priceProvider = new CurrencyAdjustedPriceProvider(priceProvider, currencyConverter.getInvertedCurrencyConverter());
		BigDecimal price;
		if (getYear() >= new Day().year) {
			price =  priceProvider.getPrice(new Day()).getClose();
		} else {
			price = avgPrice(priceProvider, getYear());
		}
		BigDecimal eps = super.getEarningsPerShare();
		if (eps.signum() == 0) {
			return BigDecimal.ZERO;
		}
		return price.divide(eps, new MathContext(10, RoundingMode.HALF_EVEN));
	}

}

