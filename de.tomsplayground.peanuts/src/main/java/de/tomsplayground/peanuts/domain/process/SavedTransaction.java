package de.tomsplayground.peanuts.domain.process;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.base.INamedElement;
import de.tomsplayground.util.Day;

@XStreamAlias("saved-transcation")
public class SavedTransaction implements INamedElement {

	private final String name;
	private final Transaction transaction;
	
	private final boolean automaticExecution;
	private final Day start;
	private Day lastExecution;
	
	public SavedTransaction(String name, Transaction transaction) {
		this(name, transaction, null);
	}
	
	public SavedTransaction(String name, Transaction transaction, Day start) {
		if (name == null) throw new IllegalArgumentException("name");
		if (transaction == null) throw new IllegalArgumentException("transaction");
		this.name = name;
		this.transaction = transaction;
		this.automaticExecution = (start != null);
		this.start = start;
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
	
	public void setLastExecution(Day lastExecution) {
		this.lastExecution = lastExecution;
	}

	public boolean isAutomaticExecution() {
		return automaticExecution;
	}
	
	@Override
	public String getName() {
		return name;
	}
}
