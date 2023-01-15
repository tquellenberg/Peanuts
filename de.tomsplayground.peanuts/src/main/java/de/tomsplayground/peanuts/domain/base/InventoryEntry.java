package de.tomsplayground.peanuts.domain.base;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction.Type;
import de.tomsplayground.peanuts.domain.process.SplitAdjustedTransactionProvider;
import de.tomsplayground.peanuts.domain.process.StockSplit;
import de.tomsplayground.peanuts.domain.reporting.investment.AnalyzedInvestmentTransaction;
import de.tomsplayground.peanuts.domain.reporting.investment.AnalyzerFactory;
import de.tomsplayground.peanuts.domain.reporting.investment.IAnalyzer;
import de.tomsplayground.peanuts.domain.statistics.XIRR;
import de.tomsplayground.peanuts.util.Day;

public class InventoryEntry {

	private final Security security;
	private final IPriceProvider priceprovider;

	private final List<InvestmentTransaction> transactions = new ArrayList<>();
	
	private BigDecimal quantity = BigDecimal.ZERO;

	private SplitAdjustedTransactionProvider splitAdjustedTransactionProvider;
	
	private Day day;

	public InventoryEntry(Security security, Day day, IPriceProvider priceprovider, ImmutableList<StockSplit> stockSplits) {
		this.security = security;
		this.day = day;
		this.priceprovider = priceprovider;
		splitAdjustedTransactionProvider = new SplitAdjustedTransactionProvider(stockSplits);
		splitAdjustedTransactionProvider.setDate(day);
	}

	public Security getSecurity() {
		return security;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public void add(InvestmentTransaction t, AnalyzerFactory analizerFactory) {
		if (t.getSecurity() != security) {
			throw new IllegalArgumentException("S:" + security.getName() + " != " +
				t.getSecurity().getName());
		}
		// Split adjustment
		t = splitAdjustedTransactionProvider.adjust(t);
		// Analyzing transaction
		Type type = t.getType();
		if (type == InvestmentTransaction.Type.BUY ||
				type == InvestmentTransaction.Type.SELL) {
			if (analizerFactory != null) {
				Iterable<InvestmentTransaction> transations = Iterables.concat(
						transactions,
						ImmutableList.of(t));
				IAnalyzer analizer = analizerFactory.getAnalizer();
				t = Iterables.getLast(analizer.getAnalyzedTransactions(transations));
			}
		}
		// Add transaction
		transactions.add(t);
		// Update quantity
		if (t.getType() == InvestmentTransaction.Type.BUY) {
			quantity = quantity.add(t.getQuantity());
		} else if (t.getType() == InvestmentTransaction.Type.SELL) {
			quantity = quantity.subtract(t.getQuantity());
		}
	}

	public ImmutableList<InvestmentTransaction> getTransactions() {
		return ImmutableList.copyOf(transactions);
	}

	public BigDecimal getGaining() {
		BigDecimal gaining = BigDecimal.ZERO;
		for (InvestmentTransaction t : transactions) {
			if (t instanceof AnalyzedInvestmentTransaction at) {
				gaining = gaining.add(at.getGain());
			}
		}
		return gaining;
	}

	public BigDecimal getDayChange() {
		BigDecimal priceChange = priceprovider.getPrice(day).getValue()
				.subtract(priceprovider.getPrice(day.addDays(-1)).getValue());
		return priceChange.multiply(quantity);
	}

	public IPrice getPrice() {
		if (priceprovider != null) {
			return priceprovider.getPrice(day);
		}
		return null;
	}

	public BigDecimal getMarketValue() {
		if (priceprovider != null && quantity.signum() != 0) {
			return priceprovider.getPrice(day).getValue().multiply(quantity);
		}
		return BigDecimal.ZERO;
	}

	IPriceProvider getPriceprovider() {
		return priceprovider;
	}

	private AnalyzedInvestmentTransaction getLastAnalyzedInvestmentTransaction() {
		for (InvestmentTransaction t : Lists.reverse(transactions)) {
			if (t instanceof AnalyzedInvestmentTransaction analyzedT) {
				return analyzedT;
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

	public BigDecimal getXIRR() {
		XIRR xirr = new XIRR();
		BigDecimal addings = BigDecimal.ZERO;
		for (InvestmentTransaction transaction : transactions) {
			if (transaction.getDay().after(day)) {
				break;
			}
			if (transaction.getType() == Type.BUY || transaction.getType() == Type.SELL) {
				xirr.add(transaction.getDay(), transaction.getAmount());
				if (transaction instanceof AnalyzedInvestmentTransaction at) {
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
		xirr.add(day, getMarketValue().add(addings));
		return xirr.calculateValue();
	}

}
