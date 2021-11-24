package de.tomsplayground.peanuts.domain.reporting.transaction;

import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

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
import de.tomsplayground.peanuts.domain.currenncy.Currencies;
import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.peanuts.domain.query.IQuery;
import de.tomsplayground.peanuts.util.Day;

@XStreamAlias("report")
public class Report extends ObservableModelObject implements ITransactionProvider, INamedElement, IConfigurable {

	private String name;
	final private Set<Account> accounts = new HashSet<Account>();
	final private Set<IQuery> queries = new HashSet<IQuery>();
	final private Map<String, String> displayConfiguration = new HashMap<String, String>();

	private transient ImmutableList<ITransaction> result;
	private transient BalanceCache balanceCache;


	private final PropertyChangeListener accountChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(java.beans.PropertyChangeEvent evt) {
			if (evt.getPropertyName().equals("balance") || evt.getPropertyName().equals("transactions")) {
				if (evt.getPropertyName().equals("transactions")) {
					result = null;
				}
				getPropertyChangeSupport().firePropertyChange(evt);
			}
		}
	};

	public Report(String name) {
		this.name = name;
	}

	public void setAccounts(Collection<Account> accounts) {
		if (CollectionUtils.isEqualCollection(this.accounts, accounts)) {
			return;
		}
		removeListener();
		Set<Account> oldAccounts = new HashSet<Account>(this.accounts);
		this.accounts.clear();
		this.accounts.addAll(accounts);
		addListener();
		firePropertyChange("accounts", oldAccounts, accounts);
		result = null;
		balanceCache = null;
	}

	private void removeListener() {
		for (Account account : accounts) {
			account.removePropertyChangeListener(accountChangeListener);
		}
	}

	private void addListener() {
		for (Account account : accounts) {
			account.addPropertyChangeListener(accountChangeListener);
		}
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
		removeListener();
		result = null;
		balanceCache = null;
	}

	public void clearQueries() {
		Set<IQuery> oldQueries = new HashSet<IQuery>(queries);
		queries.clear();
		firePropertyChange("queries", oldQueries, null);
		removeListener();
		result = null;
		balanceCache = null;
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
		// In reports, the transactions are always flat.
		return getFlatTransactions();
	}

	@Override
	public ImmutableList<ITransaction> getFlatTransactions() {
		if (result == null) {
			ImmutableList<ITransaction> transactions = AccountManager.getFlatTransactions(accounts);
			result = transactions.parallelStream()
				.filter(queryPredicate())
				.sorted(AccountManager.DAY_COMPARATOR)
				.collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
			balanceCache = new BalanceCache(result);
		}
		return result;
	}

	private Predicate<ITransaction> queryPredicate() {
		if (queries.isEmpty()) {
			return t -> true;
		} else {
			Iterator<IQuery> iterator = queries.iterator();
			Predicate<ITransaction> p = iterator.next().getPredicate();
			while (iterator.hasNext()) {
				p = p.and(iterator.next().getPredicate());
			}
			return p;
		}
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
	public Currency getCurrency() {
		return Currencies.getInstance().getDefaultCurrency();
	}

	@Override
	public BigDecimal getBalance(Day date) {
		getTransactions();
		return balanceCache.getBalance(date);
	}

	public BigDecimal getBalance(ITransaction t) {
		getTransactions();
		BigDecimal balance = balanceCache.getBalance(t);
		if (balance == null) {
			throw new IllegalArgumentException("Transaction does not belong to report:" + t);
		}
		return balance;
	}

	public ImmutableList<ITransaction> getTransactionsByDate(Day date) {
		return TransactionProviderUtil.getTransactionsByDate(getTransactions(), date, date);
	}

	@Override
	public ImmutableList<ITransaction> getTransactionsByDate(Day from, Day to) {
		return TransactionProviderUtil.getTransactionsByDate(getTransactions(), from, to);
	}

	public void reconfigureAfterDeserialization() {
		addListener();
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
