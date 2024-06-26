package de.tomsplayground.peanuts.domain.base;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.deleteWhitespace;
import static org.apache.commons.lang3.StringUtils.upperCase;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterables;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.config.ConfigurableSupport;
import de.tomsplayground.peanuts.config.IConfigurable;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.peanuts.domain.process.ITransferLocation;
import de.tomsplayground.peanuts.domain.process.Transaction;
import de.tomsplayground.peanuts.domain.process.TransferTransaction;
import de.tomsplayground.peanuts.util.Day;

@XStreamAlias("account")
public class Account extends ObservableModelObject implements ITransferLocation, ITransactionProvider,
	INamedElement, IConfigurable, IDeletable {


	private final static Logger log = LoggerFactory.getLogger(Account.class);

	private static final Splitter IBAN_SPLITTER = Splitter.fixedLength(4);
	private static final Joiner IBAN_READABLE_JOINER = Joiner.on(' ');

	public enum Type {
		UNKNOWN,
		BANK, // Bank
		ASSET, // Vermoegenswert
		LIABILITY, // Verbindlichkeit
		INVESTMENT, // Wertpapier
		CREDIT, // Kredit
		COMMODITY // Rohstoff
	}

	// Core
	private String name;
	private Currency currency;
	private BigDecimal startBalance;
	private Type type;
	private String description;
	private boolean active;
	private String iban;

	final private Map<String, String> displayConfiguration = new HashMap<String, String>();

	// Process
	final private List<Transaction> transactions = new ArrayList<Transaction>();

	transient private PropertyChangeListener transactionChangeListener = new TransactionPropertyListener();

	private final class TransactionPropertyListener implements PropertyChangeListener {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getPropertyName().equals("date")) {
				Transaction t = (Transaction) evt.getSource();
				if (transactions.remove(t)) {
					addTransactionInternal(t);
				}
			}
			if (evt.getPropertyName().equals("split")) {
				if (evt.getOldValue() != null) {
					((Transaction)evt.getOldValue()).removePropertyChangeListener(transactionChangeListener);
				}
				if (evt.getNewValue() != null) {
					((Transaction)evt.getNewValue()).addPropertyChangeListener(transactionChangeListener);
				}
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

	@Override
	public BigDecimal getBalance(Day date) {
		BigDecimal balance = transactions.parallelStream()
			.filter(t -> t.getDay().beforeOrEquals(date))
			.map(Transaction::getAmount)
			.reduce(startBalance, BigDecimal::add);
		return balance;
	}

	public BigDecimal getBalance(Transaction t) {
		BigDecimal balance = startBalance;
		for (Transaction t2 : transactions) {
			if (t2.hasSplits() && t2.getSplits().contains(t)) {
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
		firePropertyChange("deleted", Boolean.valueOf(oldActive), Boolean.valueOf(active));
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
		if (transactions.isEmpty() || 
				transactions.get(transactions.size()-1).getDay().beforeOrEquals(transaction.getDay())) {
			transactions.add(transaction);
			return;
		}
		for (int i = 0; i < transactions.size(); i++) {
			Transaction t = transactions.get(i);
			if (t.getDay().after(transaction.getDay())) {
				transactions.add(i, transaction);
				return;
			}
		}
	}

	@Override
	public void removeTransaction(Transaction transaction) {
		boolean removed = false;
		for (int i = 0; i < transactions.size(); i++) {
			Transaction t = transactions.get(i);
			if (t == transaction) {
				transaction.removePropertyChangeListener(transactionChangeListener);
				transactions.remove(i);
				firePropertyChange("transactions", transaction, null);
				removed = true;
				break;
			} else if (t.hasSplits() && t.getSplits().contains(transaction)) {
				transaction.removePropertyChangeListener(transactionChangeListener);
				t.removeSplit(transaction);
				removed = true;
				break;
			}
		}
		if (removed) {
			if (transaction instanceof TransferTransaction tt) {
				TransferTransaction complement = tt.getComplement();
				if (complement != null) {
					complement.setComplement(null);
					tt.getTarget().removeTransaction(complement);
				}
			}
			return;
		}
		throw new IllegalArgumentException("Transaction does not belong to account:" + transaction);
	}

	public int indexOf(Transaction transaction) {
		int i = 0;
		for (Transaction t : transactions) {
			if (t == transaction) {
				return i;
			}
			if (t.hasSplits() && t.getSplits().contains(transaction)) {
				return i;
			}
			i++;
		}
		throw new IllegalArgumentException("Transaction does not belong to account:" + transaction);
	}

	@Override
	public ImmutableList<ITransaction> getTransactions() {
		return ImmutableList.<ITransaction>copyOf(transactions);
	}

	@Override
	public ImmutableList<ITransaction> getFlatTransactions() {
		Builder<ITransaction> builder = ImmutableList.builder();
		for (Transaction transaction : transactions) {
			if (transaction.hasSplits()) {
				builder.addAll(transaction.getSplits());
			} else {
				builder.add(transaction);
			}
		}
		return builder.build();
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
		return TransactionProviderUtil.getTransactionsByDate(getTransactions(), from, to);
	}

	@Override
	public ImmutableList<ITransaction> getFlatTransactionsByDate(Day from, Day to) {
		return TransactionProviderUtil.getTransactionsByDate(getFlatTransactions(), from, to);
	}
	
	@Override
	public Day getMaxDate() {
		if (transactions.isEmpty()) {
			return null;
		}
		return Iterables.getLast(transactions).getDay();
	}

	@Override
	public Day getMinDate() {
		if (transactions.isEmpty()) {
			return null;
		}
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
		for (Transaction transaction : ImmutableList.<Transaction>copyOf(transactions)) {
			transaction.addPropertyChangeListener(transactionChangeListener);
			transaction.reconfigureAfterDeserialization(accountManager);
		}
		Collections.sort(transactions, new Comparator<Transaction>() {
			@Override
			public int compare(Transaction o1, Transaction o2) {
				return o1.getDay().compareTo(o2.getDay());
			}
		});
		for (Transaction transaction : transactions) {
			if (transaction instanceof TransferTransaction tt) {
				ITransferLocation target = tt.getComplement().getTarget();
				if (target != this || tt.getComplement().getComplement() != tt) {
					log.error("Inconsistent transfer transaction {} <=> {}", tt, tt.getComplement());
				}
			}
		}
	}

	public boolean isSplitTransaction(Transaction split) {
		return getParentTransaction(split) != null;
	}

	public Transaction getParentTransaction(Transaction split) {
		return transactions.parallelStream()
			.filter(t -> t.hasSplits() && t.getSplits().contains(split))
			.findAny()
			.orElse(null);
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

	@Override
	public boolean isDeleted() {
		return ! active;
	}

	@Override
	public void setDeleted(boolean deleted) {
		setActive(! deleted);
	}
	
	public String getIban() {
		return defaultString(iban);
	}
	
	public String getIbanReadable() {
		return IBAN_READABLE_JOINER.join(IBAN_SPLITTER.split(getIban()));
	}
	
	public void setIban(String iban) {
		this.iban = upperCase(deleteWhitespace(defaultString(iban)), Locale.ENGLISH);
	}
}
