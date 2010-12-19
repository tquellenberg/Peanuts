package de.tomsplayground.peanuts.domain.base;

import java.util.List;

import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.util.Day;

public interface ITransactionProvider {

	public List<ITransaction> getTransactions();

	/**
	 * Return all transactions in this time range. Including from-date and to-date. Both date can be null.
	 * 
	 */
	public List<ITransaction> getTransactionsByDate(Day from, Day to);

}
