package de.tomsplayground.peanuts.domain.reporting.investment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.process.IPriceProviderFactory;
import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.peanuts.domain.process.TransferTransaction;
import de.tomsplayground.util.Day;

public class PerformanceAnalyzer {

	private List<Value> values = new ArrayList<Value>();
	private final IPriceProviderFactory priceProviderFactory;
	private final Account account;

	public static class Value {
		private final int year;
		private final BigDecimal marketValue1;
		private final BigDecimal marketValue2;
		private BigDecimal additions;
		private BigDecimal leavings;
		private Day avgDate;
		private BigDecimal investedAvg;
		public Value(int year, BigDecimal marketValue1, BigDecimal marketValue2) {
			this.year = year;
			this.marketValue1 = marketValue1;
			this.marketValue2 = marketValue2;
			this.additions = BigDecimal.ZERO;
			this.leavings = BigDecimal.ZERO;
			this.avgDate = new Day(year, 0, 1);
			this.investedAvg = BigDecimal.ZERO;
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
	}
	
	public PerformanceAnalyzer(Account account, IPriceProviderFactory priceProviderFactory) {
		this.account = account;
		this.priceProviderFactory = priceProviderFactory;
		buidMarketValues();
	}
	
	private void buidMarketValues() {
		values.clear();
		if (account.getTransactions().isEmpty())
			return;
		int year = account.getTransactions().get(0).getDay().getYear();
		int endYear = account.getTransactions().get(account.getTransactions().size() - 1).getDay().getYear();
		Day now = new Day();
		if (endYear < now.getYear()) {
			endYear = now.getYear();
		}
		Inventory inventory = new Inventory(account, priceProviderFactory);
		for (; year <= endYear; year++) {
			Day r1 = new Day(year-1, 11, 31);
			Day r2 = new Day(year, 11, 31);
			inventory.setDate(r1);
			BigDecimal marketValue1 = inventory.getMarketValue().add(account.getBalance(r1));
			inventory.setDate(r2);
			BigDecimal marketValue2 = inventory.getMarketValue().add(account.getBalance(r2));
			Value value = new Value(year, marketValue1, marketValue2);
			calculateAdditionLeaving(r1, r2,  value);
			values.add(value);
		}
	}

	private void calculateAdditionLeaving(Day from, Day to, Value value) {
		List<ITransaction> list = account.getTransactionsByDate(from, to);
		for (ITransaction transaction : list) {
			if (! transaction.getSplits().isEmpty()) {
				List<ITransaction> splits = transaction.getSplits();
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

	public List<Value> getValues() {
		return values;
	}
}
