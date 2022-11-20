package de.tomsplayground.peanuts.domain.reporting.investment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.base.ITransactionProvider;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.process.IPriceProviderFactory;
import de.tomsplayground.peanuts.domain.process.IStockSplitProvider;
import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.peanuts.domain.process.TransferTransaction;
import de.tomsplayground.peanuts.domain.statistics.XIRR;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class PerformanceAnalyzer {

	private final static BigDecimal YEAR_DAYS = new BigDecimal(360);

	private ImmutableList<YearValue> values = ImmutableList.of();
	private final IPriceProviderFactory priceProviderFactory;
	private final ITransactionProvider account;
	private final XIRR xirrFull;
	private final XIRR xirr10Year;
	private final XIRR xirr5Year;

	private IStockSplitProvider stockSplitProvider;

	public static class YearValue {
		private final int year;
		private final BigDecimal marketValueStart;
		private final BigDecimal marketValueEnd;
		private BigDecimal additions;
		private BigDecimal leavings;
		private Day avgDate;
		private BigDecimal investedAvg;
		private final XIRR xirr = new XIRR();
		private BigDecimal rate;
		
		public YearValue(int year, BigDecimal marketValueStart, BigDecimal marketValueEnd) {
			this.year = year;
			this.marketValueStart = marketValueStart;
			this.marketValueEnd = marketValueEnd;
			this.additions = BigDecimal.ZERO;
			this.leavings = BigDecimal.ZERO;
			this.avgDate = Day.of(year, 0, 1);
			this.investedAvg = BigDecimal.ZERO;
			xirr.add(Day.of(year, 0, 1), marketValueStart);
			xirr.add(Day.of(year, 11, 31), marketValueEnd.negate());
		}
		public void add(Day date, BigDecimal amount) {
			updateInvestedAvg(date);
			updateAdditionLeavings(amount);
			updateXIRR(date, amount);
		}
		private BigDecimal daysPerYear() {
			if (year == Day.today().year) {
				return new BigDecimal(Day.today().dayOfYear());
			}
			return YEAR_DAYS;
		}
		
		private void updateInvestedAvg(Day date) {
			int dayDelta = avgDate.delta(date);
			if (dayDelta > 0) {
				BigDecimal investDelta = invested().multiply(new BigDecimal(dayDelta)).divide(daysPerYear(), PeanutsUtil.MC);
				investedAvg = investedAvg.add(investDelta);
				avgDate = date;
			}
		}
		private void updateAdditionLeavings(BigDecimal amount) {
			if (amount.signum() == 1) {
				additions = additions.add(amount);
			} else {
				leavings = leavings.add(amount);
			}
		}
		private void updateXIRR(Day date, BigDecimal amount) {
			xirr.add(date, amount);
		}

		private BigDecimal invested() {
			return marketValueStart.add(additions).add(leavings);
		}

		public int getYear() {
			return year;
		}
		public BigDecimal getMarketValueStart() {
			return marketValueStart;
		}
		public BigDecimal getMarketValueEnd() {
			return marketValueEnd;
		}
		public BigDecimal getAdditions() {
			return additions;
		}
		public BigDecimal getLeavings() {
			return leavings;
		}
		public BigDecimal getGainings() {
			return marketValueEnd.subtract(marketValueStart).subtract(additions.add(leavings));
		}
		public BigDecimal getInvestedAvg() {
			return investedAvg;
		}
		public BigDecimal getGainingPercent() {
			if (getInvestedAvg().signum() != 0) {
				return getGainings().divide(getInvestedAvg(), PeanutsUtil.MC);
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

	public PerformanceAnalyzer(ITransactionProvider account, IPriceProviderFactory priceProviderFactory,
			IStockSplitProvider stockSplitProvider) {
		this.account = account;
		this.priceProviderFactory = priceProviderFactory;
		this.stockSplitProvider = stockSplitProvider;
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
		Day now = Day.today();
		if (endYear < now.year) {
			endYear = now.year;
		}
		Inventory inventory = new Inventory(account, priceProviderFactory, null, stockSplitProvider);
		List<YearValue> elements = new ArrayList<>();
		for (; year <= endYear; year++) {
			Day r1 = Day.of(year-1, 11, 31);
			Day r2 = Day.of(year,   11, 31);
			if (r2.after(now)) {
				r2 = now;
			}
			inventory.setDate(r1);
			BigDecimal marketValueStart = inventory.getMarketValue().add(account.getBalance(r1));
			inventory.setDate(r2);
			BigDecimal marketValueEnd = inventory.getMarketValue().add(account.getBalance(r2));
			YearValue value = new YearValue(year, marketValueStart, marketValueEnd);
			calculateAdditionLeaving(r1, r2,  value);
			calculateAdditionLeaving(r1, r2, xirrFull);
			if (endYear - year <= 9) {
				if (endYear - year == 9) {
					xirr10Year.add(r1, marketValueStart);
				}
				calculateAdditionLeaving(r1, r2, xirr10Year);
			}
			if (endYear - year <= 4) {
				if (endYear - year == 4) {
					xirr5Year.add(r1, marketValueStart);
				}
				calculateAdditionLeaving(r1, r2, xirr5Year);
			}
			elements.add(value);
		}
		inventory.dispose();
		xirrFull.add(now, elements.get(elements.size()-1).marketValueEnd.negate());
		xirr10Year.add(now, elements.get(elements.size()-1).marketValueEnd.negate());
		xirr5Year.add(now, elements.get(elements.size()-1).marketValueEnd.negate());
		values = ImmutableList.copyOf(elements);
	}

	private void calculateAdditionLeaving(Day from, Day to, YearValue value) {
		for (ITransaction transaction : account.getFlatTransactionsByDate(from, to)) {
			if (transaction instanceof TransferTransaction) {
				value.add(transaction.getDay(), transaction.getAmount());
			}
		}
		value.add(to, BigDecimal.ZERO);
	}

	private void calculateAdditionLeaving(Day from, Day to, XIRR xirr) {
		ImmutableList<ITransaction> list = account.getFlatTransactionsByDate(from, to);
		for (ITransaction transaction : list) {
			if (transaction instanceof TransferTransaction) {
				xirr.add(transaction.getDay(), transaction.getAmount());
			}
		}
	}


	public ImmutableList<YearValue> getValues() {
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
