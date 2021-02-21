package de.tomsplayground.peanuts.domain.base;

import java.math.BigDecimal;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction.Type;
import de.tomsplayground.peanuts.domain.reporting.investment.AnalyzedInvestmentTransaction;
import de.tomsplayground.peanuts.domain.statistics.XIRR;
import de.tomsplayground.peanuts.util.Day;

public class InventoryEntry {

	private final Security security;
	private final IPriceProvider priceprovider;

	private ImmutableList<InvestmentTransaction> transactions = ImmutableList.of();

	public InventoryEntry(Security security, IPriceProvider priceprovider) {
		this.security = security;
		this.priceprovider = priceprovider;
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
		transactions = new ImmutableList.Builder<InvestmentTransaction>().addAll(transactions).add(t).build();
	}

	public ImmutableList<InvestmentTransaction> getTransactions() {
		return transactions;
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

	public BigDecimal getChange(Day from, Day to) {
		return getMarketValue(to).subtract(getMarketValue(from.adjustWorkday()));
	}

	public IPrice getPrice(Day day) {
		if (priceprovider != null) {
			return priceprovider.getPrice(day);
		}
		return null;
	}

	public BigDecimal getMarketValue(Day day) {
		BigDecimal quantity = getQuantity();
		if (priceprovider != null && quantity.signum() != 0) {
			return priceprovider.getPrice(day).getValue().multiply(quantity);
		}
		return BigDecimal.ZERO;
	}

	public IPriceProvider getPriceprovider() {
		return priceprovider;
	}

	private AnalyzedInvestmentTransaction getLastAnalyzedInvestmentTransaction() {
		for (InvestmentTransaction t : transactions.reverse()) {
			if (t instanceof AnalyzedInvestmentTransaction) {
				return (AnalyzedInvestmentTransaction) t;
			}
		}
		return null;
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
			investedAmount = transaction.getInvestedAmount();
		}
		return investedAmount;
	}

	public BigDecimal getXIRR(Day day) {
		XIRR xirr = new XIRR();
		BigDecimal addings = BigDecimal.ZERO;
		for (InvestmentTransaction transaction : transactions) {
			if (transaction.getDay().after(day)) {
				break;
			}
			if (transaction.getType() == Type.BUY || transaction.getType() == Type.SELL) {
				xirr.add(transaction.getDay(), transaction.getAmount());
				if (transaction instanceof AnalyzedInvestmentTransaction) {
					AnalyzedInvestmentTransaction at = (AnalyzedInvestmentTransaction)transaction;
					if (at.getQuantitySum().signum() == 0) {
						// reset
						xirr = new XIRR();
						addings = BigDecimal.ZERO;
					}
				}
			} else {
				addings = addings.add(transaction.getAmount());
			}
		}
		xirr.add(day, getMarketValue(day).add(addings));
		return xirr.calculateValue();
	}

}
