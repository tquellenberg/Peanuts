package de.tomsplayground.peanuts.domain.reporting.investment;

import java.math.BigDecimal;

import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;

public class AnalyzedInvestmentTransaction extends InvestmentTransaction {

	private BigDecimal gain = BigDecimal.ZERO;
	private BigDecimal avgPrice = BigDecimal.ZERO;
	private BigDecimal quantitySum = BigDecimal.ZERO;
	private BigDecimal investedAmount = BigDecimal.ZERO;

	private AnalyzedInvestmentTransaction(InvestmentTransaction t) {
		super(t);
	}

	public static AnalyzedInvestmentTransaction createAnalyzedInvestmentTransaction(InvestmentTransaction t) {
		if (t instanceof AnalyzedInvestmentTransaction analyzedT) {
			return analyzedT;
		}
		return new AnalyzedInvestmentTransaction(t);
	}

	public void setGain(BigDecimal gain) {
		if (gain == null) {
			throw new IllegalArgumentException("gain must not be null");
		}
		this.gain = gain;
	}

	public void setAvgPrice(BigDecimal avgPrice) {
		if (avgPrice == null) {
			throw new IllegalArgumentException("avgPrice must not be null");
		}
		this.avgPrice = avgPrice;
	}

	public BigDecimal getGain() {
		return gain;
	}

	public BigDecimal getAvgPrice() {
		return avgPrice;
	}

	public BigDecimal getQuantitySum() {
		return quantitySum;
	}

	public void setQuantitySum(BigDecimal quantitySum) {
		this.quantitySum = quantitySum;
	}

	public BigDecimal getInvestedAmount() {
		return investedAmount;
	}

	public void setInvestedAmount(BigDecimal investedAmount) {
		this.investedAmount = investedAmount;
	}
}
