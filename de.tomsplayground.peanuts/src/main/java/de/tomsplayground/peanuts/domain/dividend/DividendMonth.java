package de.tomsplayground.peanuts.domain.dividend;

import java.math.BigDecimal;

import de.tomsplayground.util.Day;

public class DividendMonth implements Comparable<DividendMonth> {

	private final Day month;

	private final BigDecimal amountInDefaultCurrency;

	private BigDecimal yearlyAmount;

	private final BigDecimal nettoInDefaultCurrency;

	private BigDecimal yearlyNetto;

	public DividendMonth(Day month, BigDecimal amountInDefaultCurrency, BigDecimal nettoInDefaultCurrency) {
		this.month = month;
		this.amountInDefaultCurrency = amountInDefaultCurrency;
		this.nettoInDefaultCurrency = nettoInDefaultCurrency;
	}

	public Day getMonth() {
		return month;
	}

	public BigDecimal getAmountInDefaultCurrency() {
		return amountInDefaultCurrency;
	}

	public BigDecimal getNettoInDefaultCurrency() {
		return nettoInDefaultCurrency;
	}

	public BigDecimal getYearlyAmount() {
		return yearlyAmount;
	}

	public void setYearlyAmount(BigDecimal yearlyAmount) {
		this.yearlyAmount = yearlyAmount;
	}

	public void setYearlyNetto(BigDecimal yearlyNetto) {
		this.yearlyNetto = yearlyNetto;
	}
	public BigDecimal getYearlyNetto() {
		return yearlyNetto;
	}

	@Override
	public int compareTo(DividendMonth o) {
		return month.compareTo(o.month);
	}
}
