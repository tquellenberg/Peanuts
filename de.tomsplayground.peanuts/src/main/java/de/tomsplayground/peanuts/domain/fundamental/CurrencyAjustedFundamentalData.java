package de.tomsplayground.peanuts.domain.fundamental;

import java.math.BigDecimal;

import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;
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

}

