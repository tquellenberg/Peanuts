package de.tomsplayground.peanuts.domain.reporting.investment;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.base.ITransactionProvider;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.process.IPriceProviderFactory;
import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.peanuts.domain.process.TransferTransaction;
import de.tomsplayground.peanuts.domain.statistics.XIRR;
import de.tomsplayground.util.Day;

public class PerformanceAnalyzer {

	private ImmutableList<Value> values = ImmutableList.of();
	private final IPriceProviderFactory priceProviderFactory;
	private final ITransactionProvider account;
	private final XIRR xirrFull;
	private final XIRR xirr10Year;
	private final XIRR xirr5Year;

	public static class Value {
		private final int year;
		private final BigDecimal marketValue1;
		private final BigDecimal marketValue2;
		private BigDecimal additions;
		private BigDecimal leavings;
		private Day avgDate;
		private BigDecimal investedAvg;
		private final XIRR xirr = new XIRR();
		private BigDecimal rate;
		public Value(int year, BigDecimal marketValue1, BigDecimal marketValue2) {
			this.year = year;
			this.marketValue1 = marketValue1;
			this.marketValue2 = marketValue2;
			this.additions = BigDecimal.ZERO;
			this.leavings = BigDecimal.ZERO;
			this.avgDate = new Day(year, 0, 1);
			this.investedAvg = BigDecimal.ZERO;
			xirr.add(new Day(year, 0, 1), marketValue1);
			xirr.add(new Day(year, 11, 31), marketValue2.negate());
		}
		public void add(Day date, BigDecimal amount) {
			int dayDelta = avgDate.delta(date);
			BigDecimal investDelta = invested().multiply(new BigDecimal(dayDelta)).divide(new BigDecimal(360), RoundingMode.HALF_EVEN);
			investedAvg = investedAvg.add(investDelta);
			avgDate = date;
			if (amount.signum() == 1) {
				additions = additions.add(amount);
			} else {
				leavings = leavings.add(amount);
			}
			xirr.add(date, amount);
		}
		
		private BigDecimal invested() {
			return marketValue1.add(additions).add(leavings);
		}
		
		public int getYear() {
			return year;
		}
		public BigDecimal getMarketValue1() {
			return marketValue1;
		}
		public BigDecimal getMarketValue2() {
			return marketValue2;
		}
		public BigDecimal getAdditions() {
			return additions;
		}
		public BigDecimal getLeavings() {
			return leavings;
		}
		public BigDecimal getGainings() {
			return marketValue2.subtract(marketValue1).subtract(additions.add(leavings));
		}
		public BigDecimal getInvestedAvg() {
			return investedAvg;
		}
		public BigDecimal getGainingPercent() {
			if (getInvestedAvg().signum() != 0) {
				return getGainings().divide(getInvestedAvg(), new MathContext(10, RoundingMode.HALF_EVEN));
			}
			return BigDecimal.ZERO;
		}
		public BigDecimal getIRR() {
			if (rate == null) {
				rate = xirr.calculateValue();
			}
			return rate;
		}
	}
	
	public PerformanceAnalyzer(ITransactionProvider account, IPriceProviderFactory priceProviderFactory) {
		this.account = account;
		this.priceProviderFactory = priceProviderFactory;
		this.xirrFull = new XIRR();
		this.xirr10Year = new XIRR();
		this.xirr5Year = new XIRR();
		buidMarketValues();
	}
	
	private void buidMarketValues() {
		if (account.getMinDate() == null) {
			return;
		}
		int year = account.getMinDate().year;
		int endYear = account.getMaxDate().year;
		Day now = new Day();
		if (endYear < now.year) {
			endYear = now.year;
		}
		Inventory inventory = new Inventory(account, priceProviderFactory);
		List<Value> elements = new ArrayList<PerformanceAnalyzer.Value>();
		for (; year <= endYear; year++) {
			Day r1 = new Day(year-1, 11, 31);
			Day r2 = new Day(year, 11, 31);
			inventory.setDate(r1);
			BigDecimal marketValue1 = inventory.getMarketValue().add(account.getBalance(r1));
			inventory.setDate(r2);
			BigDecimal marketValue2 = inventory.getMarketValue().add(account.getBalance(r2));
			Value value = new Value(year, marketValue1, marketValue2);
			calculateAdditionLeaving(r1, r2,  value);
			calculateAdditionLeaving(r1, r2, xirrFull);
			if (endYear - year <= 9) {
				if (endYear -year == 9) {
					xirr10Year.add(r1, marketValue1);
				}
				calculateAdditionLeaving(r1, r2, xirr10Year);
			}
			if (endYear - year <= 4) {
				if (endYear -year == 4) {
					xirr5Year.add(r1, marketValue1);
				}
				calculateAdditionLeaving(r1, r2, xirr5Year);
			}
			elements.add(value);
		}
		xirrFull.add(now, elements.get(elements.size()-1).marketValue2.negate());
		xirr10Year.add(now, elements.get(elements.size()-1).marketValue2.negate());
		xirr5Year.add(now, elements.get(elements.size()-1).marketValue2.negate());
		values = ImmutableList.copyOf(elements);
	}

	private void calculateAdditionLeaving(Day from, Day to, Value value) {
		ImmutableList<ITransaction> list = account.getTransactionsByDate(from, to);
		for (ITransaction transaction : list) {
			ImmutableList<ITransaction> splits = transaction.getSplits();
			if (! splits.isEmpty()) {
				for (ITransaction transaction2 : splits) {
					if (transaction2 instanceof TransferTransaction) {
						value.add(transaction2.getDay(), transaction2.getAmount());
					}
				}
			} else if (transaction instanceof TransferTransaction) {
				value.add(transaction.getDay(), transaction.getAmount());
			}
		}
		value.add(to, BigDecimal.ZERO);
	}
	
	private void calculateAdditionLeaving(Day from, Day to, XIRR xirr) {
		ImmutableList<ITransaction> list = account.getTransactionsByDate(from, to);
		for (ITransaction transaction : list) {
			ImmutableList<ITransaction> splits = transaction.getSplits();
			if (! splits.isEmpty()) {
				for (ITransaction transaction2 : splits) {
					if (transaction2 instanceof TransferTransaction) {
						xirr.add(transaction2.getDay(), transaction2.getAmount());
					}
				}
			} else if (transaction instanceof TransferTransaction) {
				xirr.add(transaction.getDay(), transaction.getAmount());
			}
		}
	}


	public ImmutableList<Value> getValues() {
		return values;
	}
	
	public BigDecimal getFullGainingPercent() {
		return xirrFull.calculateValue();
	}
	public BigDecimal get10YearGainingPercent() {
		return xirr10Year.calculateValue();
	}
	public BigDecimal get5YearGainingPercent() {
		return xirr5Year.calculateValue();
	}
}
