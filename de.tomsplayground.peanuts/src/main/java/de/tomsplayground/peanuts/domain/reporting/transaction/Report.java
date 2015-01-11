package de.tomsplayground.peanuts.domain.reporting.transaction;

import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.config.ConfigurableSupport;
import de.tomsplayground.peanuts.config.IConfigurable;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.INamedElement;
import de.tomsplayground.peanuts.domain.base.ITransactionProvider;
import de.tomsplayground.peanuts.domain.base.TransactionProviderUtil;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.peanuts.domain.query.IQuery;
import de.tomsplayground.util.Day;

@XStreamAlias("report")
public class Report extends ObservableModelObject implements ITransactionProvider, INamedElement, IConfigurable {

	private String name;
	final private Set<Account> accounts = new HashSet<Account>();
	final private Set<IQuery> queries = new HashSet<IQuery>();
	final private Map<String, String> displayConfiguration = new HashMap<String, String>();

	private transient ImmutableList<ITransaction> result;

	private final PropertyChangeListener accountChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(java.beans.PropertyChangeEvent evt) {
			result = null;
			getPropertyChangeSupport().firePropertyChange(evt);
		}
	};

	public Report(String name) {
		this.name = name;
	}

	// FIXME: wird nicht beachtet
	public boolean allAccounts() {
		return accounts.isEmpty();
	}

	public void setAccounts(Collection<Account> accounts) {
		if (CollectionUtils.isEqualCollection(this.accounts, accounts)) {
			return;
		}
		Set<Account> oldAccounts = new HashSet<Account>(this.accounts);
		for (Account account : oldAccounts) {
			account.removePropertyChangeListener(accountChangeListener);
		}
		this.accounts.clear();
		this.accounts.addAll(accounts);
		for (Account account : this.accounts) {
			account.addPropertyChangeListener(accountChangeListener);
		}
		firePropertyChange("accounts", oldAccounts, accounts);
		result = null;
	}

	public Set<Account> getAccounts() {
		return Collections.unmodifiableSet(accounts);
	}

	public Set<IQuery> getQueries() {
		return Collections.unmodifiableSet(queries);
	}

	public void addQuery(IQuery query) {
		queries.add(query);
		firePropertyChange("query", null, query);
		result = null;
	}

	public void clearQueries() {
		Set<IQuery> oldQueries = new HashSet<IQuery>(queries);
		queries.clear();
		firePropertyChange("queries", oldQueries, null);
		result = null;
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

	@Override
	public ImmutableList<ITransaction> getTransactions() {
		if (result == null) {
			Collection<ITransaction> transactions = new ArrayList<ITransaction>();
			for (ITransaction transaction : AccountManager.getTransactions(accounts)) {
				List<ITransaction> splits = transaction.getSplits();
				if (! splits.isEmpty()) {
					transactions.addAll(splits);
				} else {
					transactions.add(transaction);
				}
			}
			for (IQuery query : queries) {
				transactions = Collections2.filter(transactions, query.getPredicate());
			}
			result = ImmutableList.copyOf(transactions);
		}
		return result;
	}

	@Override
	public Day getMaxDate() {
		ImmutableList<ITransaction> transactions = getTransactions();
		if (transactions.isEmpty()) {
			return null;
		}
		return Iterables.getLast(transactions).getDay();
	}

	@Override
	public Day getMinDate() {
		ImmutableList<ITransaction> transactions = getTransactions();
		if (transactions.isEmpty()) {
			return null;
		}
		return transactions.get(0).getDay();
	}

	@Override
	public BigDecimal getBalance(Day date) {
		BigDecimal balance = BigDecimal.ZERO;
		for (Account account : accounts) {
			balance = balance.add(account.getBalance(date));
		}
		return balance;
	}

	@Override
	public Currency getCurrency() {
		return Currency.getInstance("EUR");
	}

	public BigDecimal getBalance(ITransaction t) {
		BigDecimal balance = BigDecimal.ZERO;
		for (ITransaction t2 : getTransactions()) {
			balance = balance.add(t2.getAmount());
			if (t == t2) {
				return balance;
			}
		}
		throw new IllegalArgumentException("Transaction does not belong to report:" + t);
	}

	public ImmutableList<ITransaction> getTransactionsByDate(Day date) {
		return TransactionProviderUtil.getTransactionsByDate(this, date, date);
	}

	@Override
	public ImmutableList<ITransaction> getTransactionsByDate(Day from, Day to) {
		return TransactionProviderUtil.getTransactionsByDate(this, from, to);
	}

	public void reconfigureAfterDeserialization() {
		// not used
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
