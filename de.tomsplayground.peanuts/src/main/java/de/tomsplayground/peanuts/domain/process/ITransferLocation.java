package de.tomsplayground.peanuts.domain.process;

import java.util.Currency;
import java.util.List;

import de.tomsplayground.util.Day;

public interface ITransferLocation {

	Currency getCurrency();

	List<ITransaction> getTransactionsByDate(Day date);

	String getName();

	public void addTransaction(Transaction transaction);

	public void removeTransaction(Transaction transaction);

}
