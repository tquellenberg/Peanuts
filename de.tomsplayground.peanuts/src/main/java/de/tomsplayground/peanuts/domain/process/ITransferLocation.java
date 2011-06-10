package de.tomsplayground.peanuts.domain.process;

import java.util.Currency;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.util.Day;

public interface ITransferLocation {

	Currency getCurrency();

	ImmutableList<ITransaction> getTransactionsByDate(Day date);

	String getName();

	public void addTransaction(Transaction transaction);

	public void removeTransaction(Transaction transaction);

}
