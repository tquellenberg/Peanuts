package de.tomsplayground.peanuts.domain.base;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.config.ConfigurableSupport;
import de.tomsplayground.peanuts.config.IConfigurable;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.peanuts.domain.process.ITransferLocation;
import de.tomsplayground.peanuts.domain.process.Transaction;
import de.tomsplayground.util.Day;

@XStreamAlias("account")
public class Account extends ObservableModelObject implements ITransferLocation, ITransactionProvider, INamedElement, IConfigurable {

	public enum Type {
		UNKNOWN,
		BANK, // Bank
		ASSET, // Vermögenswert
		LIABILITY, // Verbindlichkeit
		INVESTMENT, // Wertpapier
		CREDIT
	}

	// Core
	private String name;
	private Currency currency;
	private BigDecimal startBalance;
	private Type type;
	private String description;
	private boolean active;

	final private Map<String, String> displayConfiguration = new HashMap<String, String>();

	// Process
	final private List<Transaction> transactions = new LinkedList<Transaction>();
	
	transient private PropertyChangeListener transactionChangeListener = new TransactionPropertyListener();

	private class TransactionPropertyListener implements PropertyChangeListener {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getPropertyName().equals("date")) {
				Transaction t = (Transaction) evt.getSource();
				transactions.remove(t);
				addTransactionInternal(t);
			}
			if (evt.getPropertyName().equals("split")) {
				if (evt.getOldValue() != null)
					((Transaction)evt.getOldValue()).removePropertyChangeListener(transactionChangeListener);
				if (evt.getNewValue() != null)
					((Transaction)evt.getNewValue()).addPropertyChangeListener(transactionChangeListener);
			}
			if (evt.getPropertyName().equals("amount")) {
				firePropertyChange("balance", null, null);
			}
			// Propagate all transaction changes to account property listener
			getPropertyChangeSupport().firePropertyChange(evt);
		}
	}

	protected Account(String name, Currency currency, BigDecimal startBalance, Type accountType,
		String description) {
		this.name = name;
		this.currency = currency;
		this.startBalance = startBalance;
		this.type = accountType;
		this.description = description;
		this.active = true;
	}

	public void reset() {
		startBalance = BigDecimal.ZERO;
		for (Transaction t : transactions) {
			t.removePropertyChangeListener(transactionChangeListener);
		}
		transactions.clear();
		firePropertyChange("transactions", null, null);
	}

	public BigDecimal getBalance() {
		BigDecimal balance = startBalance;
		for (Transaction t : transactions) {
			balance = balance.add(t.getAmount());
		}
		return balance;
	}

	public BigDecimal getBalance(Day date) {
		BigDecimal balance = startBalance;
		for (Transaction t : transactions) {
			if (t.getDay().compareTo(date) > 0)
				break;
			balance = balance.add(t.getAmount());
		}
		return balance;		
	}
	
	public BigDecimal getBalance(Transaction t) {
		BigDecimal balance = startBalance;
		for (Transaction t2 : transactions) {
			if (t2.getSplits().contains(t)) {
				for (ITransaction t3 : t2.getSplits()) {
					balance = balance.add(t3.getAmount());
					if (t == t3) {
						return balance;
					}
				}
			}
			balance = balance.add(t2.getAmount());
			if (t == t2) {
				return balance;
			}
		}
		throw new IllegalArgumentException("Transaction does not belong to account:" + t);
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		String oldName = this.name;
		this.name = name;
		firePropertyChange("name", oldName, name);
	}

	public boolean isActive() {
		return active;
	}
	
	public void setActive(boolean active) {
		boolean oldActive = this.active;
		this.active = active;
		firePropertyChange("active", Boolean.valueOf(oldActive), Boolean.valueOf(active));
	}
	
	@Override
	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		Currency oldCurrency = this.currency;
		this.currency = currency;
		firePropertyChange("currency", oldCurrency, currency);
	}

	@Override
	public void addTransaction(Transaction transaction) {
		addTransactionInternal(transaction);
		transaction.addPropertyChangeListener(transactionChangeListener);
		firePropertyChange("transactions", null, transaction);
	}

	public void addTransactionInternal(Transaction transaction) {
		ListIterator<Transaction> iter = transactions.listIterator();
		while (iter.hasNext()) {
			Transaction t = iter.next();
			if (t.getDay().after(transaction.getDay())) {
				iter.previous();
				break;
			}
		}
		iter.add(transaction);
	}

	@Override
	public void removeTransaction(Transaction transaction) {
		ListIterator<Transaction> iter = transactions.listIterator();
		while (iter.hasNext()) {
			Transaction t = iter.next();
			if (t.equals(transaction)) {
				t.removePropertyChangeListener(transactionChangeListener);
				iter.remove();
				firePropertyChange("transactions", transaction, null);
				return;
			} else if (t.getSplits().contains(transaction)) {
				transaction.removePropertyChangeListener(transactionChangeListener);
				t.removeSplit(transaction);
				return;
			}
		}
		throw new IllegalArgumentException("Transaction does not belong to account:" + transaction);
	}

	public int indexOf(Transaction transaction) {
		int i = 0;
		for (Transaction t : transactions) {
			if (t == transaction)
				return i;
			if (t.getSplits().contains(transaction))
				return i;
			i++;
		}
		throw new IllegalArgumentException("Transaction does not belong to account:" + transaction);
	}

	@Override
	public ImmutableList<ITransaction> getTransactions() {
		return ImmutableList.<ITransaction>copyOf(transactions);
	}

	/**
	 * Return all transactions of this single day
	 * 
	 */
	@Override
	public ImmutableList<ITransaction> getTransactionsByDate(Day date) {
		return getTransactionsByDate(date, date);
	}

	/**
	 * Return all transactions in this time range.
	 * Including from-date and including to-date.
	 * Both date can be null.
	 * 
	 */
	@Override
	public ImmutableList<ITransaction> getTransactionsByDate(Day from, Day to) {
		return TransactionProviderUtil.getTransactionsByDate(this, from, to);
	}
	
	@Override
	public Day getMaxDate() {
		if (transactions.isEmpty())
			return null;
		return Iterables.getLast(transactions).getDay();
	}
	
	@Override
	public Day getMinDate() {
		if (transactions.isEmpty())
			return null;
		return transactions.get(0).getDay();
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		String oldDescription = this.description;
		this.description = description;
		firePropertyChange("description", oldDescription, description);
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		Type oldType = this.type;
		this.type = type;
		firePropertyChange("type", oldType, type);
	}

	public void reconfigureAfterDeserialization(AccountManager accountManager) {
		transactionChangeListener = new TransactionPropertyListener();
		for (Transaction transaction : transactions) {
			transaction.addPropertyChangeListener(transactionChangeListener);
			transaction.reconfigureAfterDeserialization(accountManager);
		}
	}

	public boolean isSplitTransaction(Transaction split) {
		return getParentTransaction(split) != null;
	}

	public Transaction getParentTransaction(Transaction split) {
		for (Transaction t : transactions) {
			if (t.getSplits().contains(split))
				return t;
		}
		return null;
	}

	private transient ConfigurableSupport configurableSupport;
	
	private ConfigurableSupport getConfigurableSupport() {
		if (configurableSupport == null) {
			configurableSupport = new ConfigurableSupport(displayConfiguration, getPropertyChangeSupport());
		}
		return configurableSupport;
	}
	
	@Override
	public String getConfigurationValue(String key) {
		return getConfigurableSupport().getConfigurationValue(key);
	}

	@Override
	public void putConfigurationValue(String key, String value) {
		getConfigurableSupport().putConfigurationValue(key, value);
	}
}
