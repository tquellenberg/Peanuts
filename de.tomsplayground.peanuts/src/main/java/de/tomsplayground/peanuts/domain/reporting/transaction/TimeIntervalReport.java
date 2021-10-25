package de.tomsplayground.peanuts.domain.reporting.transaction;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.base.ITransactionProvider;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.domain.process.IPriceProviderFactory;
import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.peanuts.domain.process.TransferTransaction;
import de.tomsplayground.peanuts.util.Day;

public class TimeIntervalReport extends ObservableModelObject {

	public enum Interval {
		DAY, MONTH, QUARTER, YEAR, DECADE
	}

	final private Interval interval;
	final private ImmutableList<ITransaction> transactions;
	final private Inventory inventory;

	final private Day start;
	final private Day end;

	final private List<BigDecimal> values = new ArrayList<BigDecimal>();
	final private List<BigDecimal> inventoryValues = new ArrayList<BigDecimal>();
	final private List<BigDecimal> investmentValues = new ArrayList<BigDecimal>();

	PropertyChangeListener inventoriyListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			calculateValues();
			firePropertyChange("values", null, null);
		}
	};

	public TimeIntervalReport(ITransactionProvider account, Interval interval) {
		this(account, interval, null);
	}

	public void dispose() {
		inventory.removePropertyChangeListener(inventoriyListener);
		inventory.dispose();
	}

	public TimeIntervalReport(ITransactionProvider account, Interval interval, IPriceProviderFactory priceProviderFactory) {
		this.interval = interval;
		this.transactions = account.getTransactions();
		this.inventory = new Inventory(account, priceProviderFactory);
		inventory.addPropertyChangeListener(inventoriyListener);

		if (transactions.isEmpty()) {
			start = Day.today();
			end = Day.today();
		} else {
			Day startDay = transactions.get(0).getDay();
			switch (interval) {
				case DAY:
					break;
				case MONTH:
					startDay = new Day(startDay.year, startDay.month, 1);
					break;
				case QUARTER:
					int month = startDay.month;
					month = month - (month % 3);
					startDay = new Day(startDay.year, month, 1);
					break;
				case YEAR:
					startDay = new Day(startDay.year, 0, 1);
					break;
				case DECADE:
					int year = startDay.year;
					year = year - (year % 10);
					startDay = new Day(year, 0, 1);
					break;
			}
			start = startDay;

			Day lastDay = transactions.get(transactions.size()-1).getDay();
			if (lastDay.after(Day.today())) {
				end = lastDay;
			} else {
				end = Day.today();
			}
			calculateValues();
		}
	}

	private void calculateValues() {
		synchronized (inventoryValues) {
			DateIterator dateIterator = dateIterator();
			values.clear();
			inventoryValues.clear();
			Iterator<? extends ITransaction> iterator = transactions.iterator();
			ITransaction transaction = iterator.hasNext()?iterator.next():null;
			BigDecimal investedSum = BigDecimal.ZERO;
			while (dateIterator.hasNext()) {
				BigDecimal sum = BigDecimal.ZERO;
				dateIterator.next();
				Day to = dateIterator.currentRangeEnd();
				while (transaction != null && transaction.getDay().before(to)) {
					// total amount
					sum = sum.add(transaction.getAmount());
					// investment account: adding and leaving
					if (! transaction.getSplits().isEmpty()) {
						List<ITransaction> splits = transaction.getSplits();
						for (ITransaction iTransaction : splits) {
							if (iTransaction instanceof TransferTransaction) {
								investedSum = investedSum.add(iTransaction.getAmount());
							}
						}
					} else if (transaction instanceof TransferTransaction) {
						investedSum = investedSum.add(transaction.getAmount());
					}
					// next
					transaction = iterator.hasNext()?iterator.next():null;
				}
				values.add(sum);
				investmentValues.add(investedSum);
				inventory.setDate(to.addDays(-1));
				inventoryValues.add(inventory.getMarketValue());
			}
		}
	}

	public Day getEnd() {
		return end;
	}

	public Day getStart() {
		return start;
	}

	public DateIterator dateIterator() {
		return new DateIterator(start, end, interval);
	}

	public List<BigDecimal> getValues() {
		synchronized (inventoryValues) {
			return new ArrayList<BigDecimal>(values);
		}
	}

	public List<BigDecimal> getInventoryValues() {
		synchronized (inventoryValues) {
			return new ArrayList<BigDecimal>(inventoryValues);
		}
	}

	public List<BigDecimal> getInvestmentValues() {
		synchronized (inventoryValues) {
			return new ArrayList<BigDecimal>(investmentValues);
		}
	}
}
