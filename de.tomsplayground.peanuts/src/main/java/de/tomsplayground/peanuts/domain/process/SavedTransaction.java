package de.tomsplayground.peanuts.domain.process;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.INamedElement;
import de.tomsplayground.peanuts.util.Day;

@XStreamAlias("saved-transcation")
public class SavedTransaction implements INamedElement {

	private final String name;
	private final Transaction transaction;

	private final boolean automaticExecution;
	private final Day start;
	private final Account account;

	private Day lastExecution;

	public SavedTransaction(String name, Transaction transaction) {
		this(name, transaction, null, null);
	}

	public SavedTransaction(String name, Transaction transaction, Day start, Account account) {
		if (name == null) throw new IllegalArgumentException("name");
		if (transaction == null) throw new IllegalArgumentException("transaction");
		this.name = name;
		this.transaction = transaction;
		this.automaticExecution = (start != null);
		this.start = start;
		this.account = account;
	}

	public Transaction getTransaction() {
		return transaction;
	}

	public Day getStart() {
		return start;
	}

	public Day getLastExecution() {
		return lastExecution;
	}

	public boolean isAutomaticExecution() {
		return automaticExecution;
	}

	public Account getAccount() {
		return account;
	}

	@Override
	public String getName() {
		return name;
	}
}
