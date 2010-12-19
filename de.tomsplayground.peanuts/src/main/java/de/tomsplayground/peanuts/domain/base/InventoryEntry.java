package de.tomsplayground.peanuts.domain.base;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.reporting.investment.AnalyzedInvestmentTransaction;
import de.tomsplayground.util.Day;

public class InventoryEntry {

	private final Security security;
	private final List<InvestmentTransaction> transactions = new ArrayList<InvestmentTransaction>();
	private final IPriceProvider priceprovider;
	private Day day;

	public InventoryEntry(Security security, IPriceProvider priceprovider, Day day) {
		this.security = security;
		this.priceprovider = priceprovider;
		this.day = day;
		if (priceprovider != null && day == null)
			throw new IllegalArgumentException("day");
	}

	public Security getSecurity() {
		return security;
	}

	public BigDecimal getQuantity() {
		BigDecimal quantity = BigDecimal.ZERO;
		for (InvestmentTransaction t : transactions) {
			if (t.getType() == InvestmentTransaction.Type.BUY) {
				quantity = quantity.add(t.getQuantity());
			} else if (t.getType() == InvestmentTransaction.Type.SELL) {
				quantity = quantity.subtract(t.getQuantity());
			}
		}
		return quantity;
	}

	public void add(InvestmentTransaction t) {
		if (t.getSecurity() != security) {
			throw new IllegalArgumentException("S:" + security.getName() + " != " +
				t.getSecurity().getName());
		}
		transactions.add(t);
	}

	public List<InvestmentTransaction> getTransations() {
		return new ArrayList<InvestmentTransaction>(transactions);
	}

	public BigDecimal getGaining() {
		BigDecimal gaining = BigDecimal.ZERO;
		for (InvestmentTransaction t : transactions) {
			if (t instanceof AnalyzedInvestmentTransaction) {
				AnalyzedInvestmentTransaction at = (AnalyzedInvestmentTransaction) t;
				gaining = gaining.add(at.getGain());
			}
		}
		return gaining;
	}

	public Price getPrice() {
		if (priceprovider != null)
			return priceprovider.getPrice(day);
		return null;
	}
	
	public BigDecimal getMarketValue() {
		if (priceprovider != null && getQuantity().compareTo(BigDecimal.ZERO) != 0)
			return getPrice().getValue().multiply(getQuantity());
		return BigDecimal.ZERO;
	}

	public void setDate(Day day) {
		if (priceprovider != null && day == null)
			throw new IllegalArgumentException("day");
		this.day = day;
	}

	public IPriceProvider getPriceprovider() {
		return priceprovider;
	}

	private AnalyzedInvestmentTransaction getLastAnalyzedInvestmentTransaction() {
		AnalyzedInvestmentTransaction result = null;
		for (InvestmentTransaction t : transactions) {
			if (t instanceof AnalyzedInvestmentTransaction) {
				result = (AnalyzedInvestmentTransaction) t;
			}
		}
		return result;
	}
	
	public BigDecimal getAvgPrice() {
		AnalyzedInvestmentTransaction transaction = getLastAnalyzedInvestmentTransaction();
		if (transaction != null) {
			return transaction.getAvgPrice();
		}
		return null;
	}
	
	public BigDecimal getInvestedAmount() {
		BigDecimal investedAmount = BigDecimal.ZERO;
		AnalyzedInvestmentTransaction transaction = getLastAnalyzedInvestmentTransaction();
		if (transaction != null) {
			investedAmount = getQuantity().multiply(transaction.getAvgPrice());
		}
		return investedAmount;
	}

}
