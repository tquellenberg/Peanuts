package de.tomsplayground.peanuts.domain.reporting.transaction;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.peanuts.util.Day;

public class BalanceCache {
	
	// startBalance and pointer to first transaction of this year
	private record YearEntry(int year, BigDecimal startBalance, int pointer) {}

	private final Map<Integer, YearEntry> cache = new HashMap<>();
	
	private final YearEntry START;
	private final List<ITransaction> transactions;
	
	public BalanceCache(List<ITransaction> transactions) {
		this.transactions = transactions;
		int startYear;
		if (transactions.isEmpty()) {
			startYear = Day.today().year;
		} else {
			startYear = transactions.get(0).getDay().year;
		}
		this.START = new YearEntry(startYear, BigDecimal.ZERO, 0);
	}
	
	public void resetYearAndFollowing(int year) {
		for (Entry<Integer, YearEntry> e : cache.entrySet()) {
			if (e.getKey() >= year) {
				e.setValue(null);
			}
		}
	}
	
	private YearEntry getStartBalance(int year) {
		if (year <= START.year) {
			return START;
		}
		YearEntry yearEntry = cache.get(year);
		if (yearEntry != null) {
			return yearEntry;
		} else {
			yearEntry = getStartBalance(year - 1);
			int pos = yearEntry.pointer;
			BigDecimal balance = yearEntry.startBalance;
			while (pos < transactions.size()) {
				ITransaction transaction = transactions.get(pos);
				if (transaction.getDay().year == yearEntry.year) {
					balance = balance.add(transaction.getAmount());
					pos++;
				} else {
					break;
				}
			}
			YearEntry entry = new YearEntry(year, balance, pos);
			cache.put(year, entry);
			return entry;
		}
	}
	
	public BigDecimal getBalance(ITransaction t) {
		YearEntry yearEntry = getStartBalance(t.getDay().year);
		BigDecimal balance = yearEntry.startBalance;
		int pos = yearEntry.pointer;
		while (pos < transactions.size()) {
			ITransaction transaction = transactions.get(pos++);
			balance = balance.add(transaction.getAmount());
			if (transaction == t) {
				return balance;
			}
		}
		return null;
	}
	
	public BigDecimal getBalance(Day date) {
		YearEntry yearEntry = getStartBalance(date.year);
		BigDecimal balance = yearEntry.startBalance;
		int pos = yearEntry.pointer;
		while (pos < transactions.size()) {
			ITransaction transaction = transactions.get(pos++);
			if (transaction.getDay().after(date)) {
				return balance;
			}
			balance = balance.add(transaction.getAmount());
		}
		return balance;
	}

}
