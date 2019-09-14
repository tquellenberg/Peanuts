package de.tomsplayground.peanuts.domain.dividend;

import java.math.BigDecimal;

import de.tomsplayground.util.Day;

public class DividendMonth implements Comparable<DividendMonth> {

	private final Day month;

	private final BigDecimal amountInDefaultCurrency;

	private BigDecimal yearlyAmount;

	private final BigDecimal nettoInDefaultCurrency;

	private BigDecimal yearlyNetto;

	private final BigDecimal futureAmountInDefaultCurrency;

	private BigDecimal futureYearlyAmount;

	public DividendMonth(Day month, BigDecimal amountInDefaultCurrency, BigDecimal nettoInDefaultCurrency,
		BigDecimal futureAmountInDefaultCurrency) {
		this.month = month;
		this.amountInDefaultCurrency = amountInDefaultCurrency;
		this.nettoInDefaultCurrency = nettoInDefaultCurrency;
		this.futureAmountInDefaultCurrency = futureAmountInDefaultCurrency;
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

	public BigDecimal getFutureAmountInDefaultCurrency() {
		return futureAmountInDefaultCurrency;
	}
	public void setFutureYearlyAmount(BigDecimal futureYearlyAmount) {
		this.futureYearlyAmount = futureYearlyAmount;
	}
	public BigDecimal getFutureYearlyAmount() {
		return futureYearlyAmount;
	}
	@Override
	public int compareTo(DividendMonth o) {
		return month.compareTo(o.month);
	}
}
