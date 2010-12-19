package de.tomsplayground.peanuts.client.editors.account;

import org.eclipse.swt.widgets.Composite;

import de.tomsplayground.peanuts.domain.process.Transaction;

public interface ITransactionDetail {

	void setInput(Transaction transaction, Transaction parentTransaction);

	Composite createComposite(Composite parent);

	Composite getComposite();

}
