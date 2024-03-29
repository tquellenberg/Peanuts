package de.tomsplayground.peanuts.domain.base;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class TransactionProviderUtil {

	/**
	 * Return all transactions in this time range.
	 * Including from-date and to-date.
	 * Both date can be null.
	 *
	 */
	public static <T extends ITransaction> ImmutableList<T> getTransactionsByDate(ImmutableList<T> transactions, Day from, Day to) {
		int fromIndex;
		if (from != null) {
			fromIndex = PeanutsUtil.binarySearch(transactions, from);
			if (fromIndex >= 0) {
				while (fromIndex > 0 && transactions.get(fromIndex - 1).getDay().equals(from)) {
					fromIndex --;
				}
			} else {
				fromIndex = -fromIndex - 1;
			}
		} else {
			fromIndex = 0;
		}
		int toIndex;
		if (to != null) {
			if (from != null && from.delta(to) < 7) {
				// Shortcut: start directly with fromIndex
				toIndex = fromIndex;
				while (toIndex < transactions.size() && transactions.get(toIndex).getDay().beforeOrEquals(to)) {
					toIndex ++;
				}
			} else {
				toIndex = PeanutsUtil.binarySearch(transactions, to);
				if (toIndex >= 0) {
					// Exact match: include equal date entries
					while (toIndex < transactions.size() && transactions.get(toIndex).getDay().beforeOrEquals(to)) {
						toIndex ++;
					}
				} else {
					// Not found: insert position
					toIndex = -toIndex - 1;
					if (toIndex > transactions.size()) {
						toIndex --;
					}
				}
			}
		} else {
			toIndex = transactions.size();
		}
		return transactions.subList(fromIndex, toIndex);
	}
}
