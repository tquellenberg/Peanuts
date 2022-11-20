package de.tomsplayground.peanuts.domain.base;

import java.math.BigDecimal;
import java.util.Currency;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.peanuts.util.Day;

public interface ITransactionProvider {

	ImmutableList<ITransaction> getTransactions();

	/**
	 * Return all transactions in this time range. Including from-date and to-date. Both date can be null.
	 * @deprecated
	 */
	ImmutableList<ITransaction> getTransactionsByDate(Day from, Day to);

	/**
	 * Return all transactions in this time range. Including from-date and to-date. Both date can be null.
	 */
	ImmutableList<ITransaction> getFlatTransactionsByDate(Day from, Day to);

	/**
	 * For split transactions, the splits are returned without the grouping transaction.
	 *
	 */
	ImmutableList<ITransaction> getFlatTransactions();

	Day getMinDate();

	Day getMaxDate();

	BigDecimal getBalance(Day date);

	Currency getCurrency();
}
