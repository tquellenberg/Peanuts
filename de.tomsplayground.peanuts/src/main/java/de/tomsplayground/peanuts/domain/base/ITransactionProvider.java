package de.tomsplayground.peanuts.domain.base;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.util.Day;

public interface ITransactionProvider {

	public ImmutableList<ITransaction> getTransactions();

	/**
	 * Return all transactions in this time range. Including from-date and to-date. Both date can be null.
	 * 
	 */
	public ImmutableList<ITransaction> getTransactionsByDate(Day from, Day to);

	Day getMinDate();

	Day getMaxDate();

}
