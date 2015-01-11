package de.tomsplayground.peanuts.domain.base;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.base.Category.Type;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.domain.process.Credit;
import de.tomsplayground.peanuts.domain.process.EuroTransactionWrapper;
import de.tomsplayground.peanuts.domain.process.ICredit;
import de.tomsplayground.peanuts.domain.process.ITimedElement;
import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.domain.process.SavedTransaction;
import de.tomsplayground.peanuts.domain.process.StockSplit;
import de.tomsplayground.peanuts.domain.process.StopLoss;
import de.tomsplayground.peanuts.domain.reporting.forecast.Forecast;
import de.tomsplayground.peanuts.domain.reporting.investment.AnalyzerFactory;
import de.tomsplayground.peanuts.domain.reporting.transaction.Report;
import de.tomsplayground.peanuts.domain.statistics.SecurityCategoryMapping;
import de.tomsplayground.util.Day;

@XStreamAlias("accountmanager")
public class AccountManager extends ObservableModelObject {

	private ImmutableList<Account> accounts = ImmutableList.of();

	private ImmutableList<Security> securities = ImmutableList.of();

	// FIXME: Do not use HashSet with mutable objects.
	final private Set<Category> categories = new HashSet<Category>();

	final private List<Report> reports = new ArrayList<Report>();

	final private List<Forecast> forecasts = new ArrayList<Forecast>();

	final private List<ICredit> credits = new ArrayList<ICredit>();

	private ImmutableList<SavedTransaction> savedTransactions = ImmutableList.of();

	private Set<StockSplit> stockSplits = new HashSet<StockSplit>();

	private Set<StopLoss> stopLosses = new HashSet<StopLoss>();

	private List<SecurityCategoryMapping> securityCategoryMappings = new ArrayList<SecurityCategoryMapping>();

	private transient Inventory fullInventory;

	private static final Comparator<INamedElement> NAMED_COMPARATOR = new Comparator<INamedElement>() {
		@Override
		public int compare(INamedElement o1, INamedElement o2) {
			return o1.getName().compareToIgnoreCase(o2.getName());
		}
	};

	private static final Comparator<ITimedElement> DAY_COMPARATOR = new Comparator<ITimedElement>() {
		@Override
		public int compare(ITimedElement o1, ITimedElement o2) {
			return o1.getDay().compareTo(o2.getDay());
		}
	};

	public Account getOrCreateAccount(String name, Account.Type type) {
		if (name == null) {
			throw new IllegalArgumentException("name");
		}
		if (type == null) {
			throw new IllegalArgumentException("type");
		}

		for (Account account : accounts) {
			if (account.getName().equals(name) &&
				(account.getType() == type || account.getType() == Account.Type.UNKNOWN || type == Account.Type.UNKNOWN)) {
				if (account.getType() == Account.Type.UNKNOWN) {
					account.setType(type);
				}
				return account;
			}
		}
		Account account = new Account(name, Currency.getInstance("EUR"), BigDecimal.ZERO, type, "");
		List<Account> l = new ArrayList<Account>(accounts);
		l.add(account);
		Collections.sort(l, NAMED_COMPARATOR);
		accounts = ImmutableList.copyOf(l);
		firePropertyChange("account", null, account);
		return account;
	}

	/**
	 *
	 * @return List of all accounts.
	 */
	public ImmutableList<Account> getAccounts() {
		return accounts;
	}

	/**
	 * @return Transactions of given accounts, sorted by date.
	 */
	public static ImmutableList<ITransaction> getTransactions(Set<Account> accountList) {
		List<ITransaction> result = new ArrayList<ITransaction>();
		for (Account account : accountList) {
			if (account.getCurrency().getCurrencyCode().equals("DEM")) {
				// Convert to EURO
				for (ITransaction t : account.getTransactions()) {
					result.add(new EuroTransactionWrapper(t, account.getCurrency()));
				}
			} else
				result.addAll(account.getTransactions());
		}
		Collections.sort(result, DAY_COMPARATOR);
		return ImmutableList.copyOf(result);
	}

	public Security getOrCreateSecurity(String name) {
		if (name == null) {
			throw new IllegalArgumentException("name");
		}
		name = name.trim();
		for (Security security : securities) {
			if (security.getName().trim().equals(name)) {
				return security;
			}
		}
		Security security = new Security(name);
		List<Security> l = new ArrayList<Security>(securities);
		l.add(security);
		Collections.sort(l, NAMED_COMPARATOR);
		securities = ImmutableList.copyOf(l);
		firePropertyChange("security", null, security);
		return security;
	}

	public ImmutableList<Security> getSecurities() {
		return securities;
	}

	public Category getOrCreateCategory(String name) {
		Category cat = getCategory(name);
		if (cat == null) {
			cat = new Category(name, Category.Type.UNKNOWN);
			categories.add(cat);
			firePropertyChange("category", null, cat);
		}
		return cat;
	}

	public Category getCategory(String name) {
		return getByName(categories, name);
	}

	public Set<Category> getCategories() {
		return Collections.unmodifiableSet(categories);
	}

	public void addCategory(Category cat) {
		Category check = getCategory(cat.getName());
		if (check != null &&
			(check.getType() == cat.getType() || check.getType() == Category.Type.UNKNOWN || cat.getType() == Category.Type.UNKNOWN)) {
			if (check.getType() == Category.Type.UNKNOWN) {
				check.setType(cat.getType());
			}
			for (Category child : cat.getChildCategories()) {
				if ( !check.hasChildCategory(child.getName())) {
					check.addChildCategory(child);
				}
			}
			return;
		}
		categories.add(cat);
		firePropertyChange("category", null, cat);
	}

	public void addReport(Report report) {
		reports.add(report);
		Collections.sort(reports, NAMED_COMPARATOR);
		firePropertyChange("report", null, report);
	}

	public List<Report> getReports() {
		return Collections.unmodifiableList(reports);
	}

	public Report getReport(String name) {
		return getByName(reports, name);
	}

	public boolean removeSavedTransaction(String name) {
		SavedTransaction toDelete = getByName(savedTransactions, name);
		if (toDelete != null) {
			List<SavedTransaction> list = new ArrayList<SavedTransaction>(savedTransactions);
			list.remove(toDelete);
			savedTransactions = ImmutableList.copyOf(list);
			firePropertyChange("savedTransaction", toDelete, null);
		}
		return toDelete != null;
	}

	public void addSavedTransaction(SavedTransaction savedTransaction) {
		List<SavedTransaction> list = new ArrayList<SavedTransaction>(savedTransactions);
		list.add(savedTransaction);
		Collections.sort(savedTransactions, NAMED_COMPARATOR);
		savedTransactions = ImmutableList.copyOf(list);
		firePropertyChange("savedTransaction", null, savedTransaction);
	}

	public ImmutableList<SavedTransaction> getSavedTransactions() {
		return savedTransactions;
	}

	public SavedTransaction getSavedTransaction(String name) {
		return getByName(savedTransactions, name);
	}

	public void addSecurityCategoryMapping(SecurityCategoryMapping securityCategoryMapping) {
		securityCategoryMappings.add(securityCategoryMapping);
		Collections.sort(securityCategoryMappings, NAMED_COMPARATOR);
		firePropertyChange("securityCategoryMapping", null, securityCategoryMapping);
	}

	public List<SecurityCategoryMapping> getSecurityCategoryMappings() {
		return Collections.unmodifiableList(securityCategoryMappings);
	}

	public SecurityCategoryMapping getSecurityCategoryMapping(String name) {
		return getByName(securityCategoryMappings, name);
	}

	public void addForecast(Forecast forecast) {
		forecasts.add(forecast);
		Collections.sort(forecasts, NAMED_COMPARATOR);
		firePropertyChange("forecast", null, forecast);
	}

	public List<Forecast> getForecasts() {
		return Collections.unmodifiableList(forecasts);
	}

	public Forecast getForecast(String name) {
		return getByName(forecasts, name);
	}

	public void addCredit(ICredit credit) {
		credits.add(credit);
		Collections.sort(credits, NAMED_COMPARATOR);
		firePropertyChange("credit", null, credit);
	}

	public List<ICredit> getCredits() {
		return Collections.unmodifiableList(credits);
	}

	public ICredit getCredit(String name) {
		return getByName(credits, name);
	}

	private <T extends INamedElement> T getByName(Iterable<T> elements, final String name) {
		return Iterables.find(elements, new Predicate<T>() {
			@Override
			public boolean apply(T element) {
				return element.getName().equals(name);
			}
		}, null);
	}

	public void reset() {
		accounts = ImmutableList.of();
		securities = ImmutableList.of();
		categories.clear();
		reports.clear();
		forecasts.clear();
		credits.clear();
		stockSplits.clear();
		stopLosses.clear();
		savedTransactions = ImmutableList.of();
		securityCategoryMappings.clear();
	}

	public void reconfigureAfterDeserialization() {
		if (stopLosses == null) {
			stopLosses = new HashSet<StopLoss>();
		}
		if (stockSplits == null) {
			stockSplits = new HashSet<StockSplit>();
		}
		if (securityCategoryMappings == null) {
			securityCategoryMappings = new ArrayList<SecurityCategoryMapping>();
		}
		if (savedTransactions == null) {
			savedTransactions = ImmutableList.of();
		}
		for (Account account : accounts) {
			account.reconfigureAfterDeserialization(this);
		}
		for (Security security : securities) {
			security.reconfigureAfterDeserialization(this);
		}
		for (Category parentCategory : categories) {
			for (Category c : parentCategory.getChildCategories()) {
				c.setParent(parentCategory);
			}
		}
		for (Report report : reports) {
			report.reconfigureAfterDeserialization();
		}
		for (Forecast forecast: forecasts) {
			forecast.reconfigureAfterDeserialization();
		}
		for (ICredit credit: credits) {
			((Credit)credit).reconfigureAfterDeserialization();
		}
	}

	public Category getCategoryByPath(String path) {
		String[] splits = path.split(" : ");
		Category cat = getCategory(splits[0]);
		for (int i = 1; i < splits.length; i++ ) {
			cat = cat.getCategory(splits[i]);
		}
		return cat;
	}

	public ImmutableSet<Category> getCategories(final Type type) {
		return ImmutableSet.copyOf(Iterables.filter(categories, new Predicate<Category>() {
			@Override
			public boolean apply(Category c) {
				return (c.getType() == type);
			}
		}));
	}

	public void removeCategory(Category categoryToRemove) {
		if (categories.contains(categoryToRemove)) {
			// Top level Category; check for transactions
			if (isCategoryUsed(categoryToRemove)) {
				throw new IllegalStateException("Can't remove category. Category is in use.");
			}
			categories.remove(categoryToRemove);
			firePropertyChange("category", categoryToRemove, null);
		} else {
			for (Category parentCat : categories) {
				if (parentCat.getChildCategories().contains(categoryToRemove)) {
					replaceAllCategories(categoryToRemove, parentCat);
					parentCat.removeChildCategory(categoryToRemove);
					firePropertyChange("category", categoryToRemove, null);
					break;
				}
			}
		}
	}

	private boolean isCategoryUsed(Category category) {
		for (Account account : accounts) {
			for (ITransaction transaction : account.getTransactions()) {
				if (category.equals(transaction.getCategory()))
					return true;
				for (ITransaction transaction2 : transaction.getSplits()) {
					if (category.equals(transaction2.getCategory()))
						return true;
				}
			}
		}
		return false;
	}

	private void replaceAllCategories(Category categoryFrom, Category categoryTo) {
		for (Account account : accounts) {
			for (ITransaction transaction : account.getTransactions()) {
				if (categoryFrom.equals(transaction.getCategory()))
					transaction.setCategory(categoryTo);
				for (ITransaction transaction2 : transaction.getSplits()) {
					if (categoryFrom.equals(transaction2.getCategory()))
						transaction2.setCategory(categoryTo);
				}
			}
		}
	}

	/**
	 * Returns all splits for the given security.
	 * The splits are ordered by date.
	 *
	 */
	public ImmutableList<StockSplit> getStockSplits(final Security security) {
		return ImmutableSortedSet.copyOf(DAY_COMPARATOR,
			Iterables.filter(stockSplits, new Predicate<StockSplit>() {
				@Override
				public boolean apply(StockSplit stockSplit) {
					return stockSplit.getSecurity().equals(security);
				}
			}
				)).asList();
	}

	public void addStockSplit(StockSplit stockSplit) {
		stockSplits.add(stockSplit);
	}

	public boolean removeStockSplit(StockSplit stockSplit) {
		return stockSplits.remove(stockSplit);
	}

	public void setStockSplits(Security security, Set<StockSplit> splits) {
		for (Iterator<StockSplit> iter = stockSplits.iterator(); iter.hasNext();) {
			StockSplit stockSplit = iter.next();
			if (stockSplit.getSecurity().equals(security)) {
				iter.remove();
			}
		}
		stockSplits.addAll(splits);
	}

	/**
	 * Returns all stop loss objects for the given security.
	 */
	public ImmutableSet<StopLoss> getStopLosses(final Security security) {
		return ImmutableSet.copyOf(
			Iterables.filter(stopLosses, new Predicate<StopLoss>() {
				@Override
				public boolean apply(StopLoss stopLoss) {
					return stopLoss.getSecurity().equals(security);
				}
			}
				));
	}

	public void addStopLoss(StopLoss stopLoss) {
		stopLosses.add(stopLoss);
		firePropertyChange("stopLoss", null, stopLoss);
	}

	public boolean removeStopLoss(StopLoss stopLoss) {
		boolean remove = stopLosses.remove(stopLoss);
		if (remove) {
			firePropertyChange("stopLoss", stopLoss, null);
		}
		return remove;
	}

	public Inventory getFullInventory() {
		synchronized (this) {
			if (fullInventory == null) {
				Report report = new Report("temp");
				report.setAccounts(getAccounts());

				fullInventory = new Inventory(report, PriceProviderFactory.getInstance(), new Day(), new AnalyzerFactory());
			}
			return fullInventory;
		}
	}
}
