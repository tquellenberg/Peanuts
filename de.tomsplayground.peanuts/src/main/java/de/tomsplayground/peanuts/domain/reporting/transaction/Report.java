package de.tomsplayground.peanuts.domain.reporting.transaction;

import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections.CollectionUtils;

import com.thoughtworks.xstream.annotations.XStreamAlias;

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
	final private Map<String, String> displayConfiguration = new ConcurrentHashMap<String, String>();
	
	private transient List<ITransaction> result;
	
	private PropertyChangeListener accountChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(java.beans.PropertyChangeEvent evt) {
			result = null;
		}
	};

	public Report(String name) {
		this.name = name;
	}

	// FIXME: wird nicht beachtet
	public boolean allAccounts() {
		return accounts.isEmpty();
	}

	public void setAccounts(Set<Account> accounts) {
		if (CollectionUtils.isEqualCollection(this.accounts, accounts))
			return;
		Set<Account> oldAccounts = new HashSet<Account>(this.accounts);
		for (Account account : oldAccounts) {
			account.removePropertyChangeListener(accountChangeListener);
		}
		this.accounts.clear();
		this.accounts.addAll(accounts);
		for (Account account : accounts) {
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
	public List<ITransaction> getTransactions() {
		if (result == null) {
			List<ITransaction> transactions = new ArrayList<ITransaction>();
			for (ITransaction transaction : AccountManager.getTransactions(accounts)) {
				List<ITransaction> splits = transaction.getSplits();
				if (! splits.isEmpty()) {
					transactions.addAll(splits);
				} else {
					transactions.add(transaction);
				}
			}
			for (IQuery query : queries) {
				transactions = query.filter(transactions);
			}
			result = transactions;
		}
		return result;
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

	public List<ITransaction> getTransactionsByDate(Day date) {
		return TransactionProviderUtil.getTransactionsByDate(this, date, date);
	}

	@Override
	public List<ITransaction> getTransactionsByDate(Day from, Day to) {
		return TransactionProviderUtil.getTransactionsByDate(this, from, to);
	}
	
	public void reconfigureAfterDeserialization() {
		// not used
	}

	@Override
	public Map<String, String> getDisplayConfiguration() {
		return displayConfiguration;
	}
}
