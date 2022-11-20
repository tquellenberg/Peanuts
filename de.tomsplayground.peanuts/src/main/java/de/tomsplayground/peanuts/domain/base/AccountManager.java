package de.tomsplayground.peanuts.domain.base;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.alarm.SecurityAlarm;
import de.tomsplayground.peanuts.domain.base.Category.Type;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.domain.calendar.CalendarEntry;
import de.tomsplayground.peanuts.domain.calendar.SecurityCalendarEntry;
import de.tomsplayground.peanuts.domain.comparision.Comparison;
import de.tomsplayground.peanuts.domain.comparision.SectorInput;
import de.tomsplayground.peanuts.domain.currenncy.Currencies;
import de.tomsplayground.peanuts.domain.process.Credit;
import de.tomsplayground.peanuts.domain.process.EuroTransactionWrapper;
import de.tomsplayground.peanuts.domain.process.ICredit;
import de.tomsplayground.peanuts.domain.process.IStockSplitProvider;
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
import de.tomsplayground.peanuts.domain.watchlist.WatchlistConfiguration;
import de.tomsplayground.peanuts.util.Day;

@XStreamAlias("accountmanager")
public class AccountManager extends ObservableModelObject implements ISecurityProvider, IStockSplitProvider {

	private ImmutableList<Account> accounts = ImmutableList.of();

	private ImmutableList<Security> securities = ImmutableList.of();

	// FIXME: Do not use HashSet with mutable objects.
	private final Set<Category> categories = new HashSet<Category>();

	private final List<Report> reports = new ArrayList<Report>();

	private final List<Forecast> forecasts = new ArrayList<Forecast>();

	private final List<ICredit> credits = new ArrayList<ICredit>();

	private ImmutableList<SavedTransaction> savedTransactions = ImmutableList.of();

	private Set<StockSplit> stockSplits = new HashSet<StockSplit>();

	private Set<StopLoss> stopLosses = new HashSet<StopLoss>();

	private List<SecurityCategoryMapping> securityCategoryMappings = new ArrayList<SecurityCategoryMapping>();

	private List<CalendarEntry> calendarEntries = new ArrayList<CalendarEntry>();

	private List<SecurityAlarm> securityAlarms = new ArrayList<>();

	private List<WatchlistConfiguration> watchlistConfigurations = new ArrayList<WatchlistConfiguration>();
	
	private List<Comparison> comparisons = new ArrayList<>();

	private transient Inventory fullInventory;

	public static final Comparator<ITimedElement> DAY_COMPARATOR = new Comparator<ITimedElement>() {
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
		Account account = new Account(name, Currencies.getInstance().getDefaultCurrency(), BigDecimal.ZERO, type, "");
		accounts = new ImmutableList.Builder<Account>().addAll(accounts).add(account).build();
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
	public static ImmutableList<ITransaction> getFlatTransactions(Set<Account> accountList) {
		List<ITransaction> result = new ArrayList<ITransaction>();
		for (Account account : accountList) {
			if (account.getCurrency().getCurrencyCode().equals("DEM")) {
				// Convert to EURO
				for (ITransaction t : account.getFlatTransactions()) {
					result.add(new EuroTransactionWrapper(t, account.getCurrency()));
				}
			} else {
				result.addAll(account.getFlatTransactions());
			}
		}
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
		securities = new ImmutableList.Builder<Security>().addAll(securities).add(security).build();
		firePropertyChange("security", null, security);
		return security;
	}

	@Override
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
		return getByNameNonDeleted(categories, name);
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
		firePropertyChange("report", null, report);
	}

	public List<Report> getReports() {
		return Collections.unmodifiableList(reports);
	}

	public Report getReport(String name) {
		return getByNameNonDeleted(reports, name);
	}

	public boolean removeSavedTransaction(SavedTransaction toDelete) {
		if (toDelete != null) {
			List<SavedTransaction> list = new ArrayList<SavedTransaction>(savedTransactions);
			if (list.remove(toDelete)) {
				savedTransactions = ImmutableList.copyOf(list);
				firePropertyChange("savedTransaction", toDelete, null);
				return true;
			}
		}
		return false;
	}

	public void addSavedTransaction(SavedTransaction savedTransaction) {
		savedTransactions = new ImmutableList.Builder<SavedTransaction>().addAll(savedTransactions).add(savedTransaction).build();
		firePropertyChange("savedTransaction", null, savedTransaction);
	}

	public ImmutableList<SavedTransaction> getSavedTransactions() {
		return savedTransactions;
	}

	public SavedTransaction getSavedTransaction(String name) {
		return getByNameNonDeleted(savedTransactions, name);
	}

	public void addSecurityCategoryMapping(SecurityCategoryMapping securityCategoryMapping) {
		securityCategoryMappings.add(securityCategoryMapping);
		firePropertyChange("securityCategoryMapping", null, securityCategoryMapping);
	}

	public List<SecurityCategoryMapping> getSecurityCategoryMappings() {
		return Collections.unmodifiableList(securityCategoryMappings);
	}

	public SecurityCategoryMapping getSecurityCategoryMapping(String name) {
		return getByNameNonDeleted(securityCategoryMappings, name);
	}

	public void addForecast(Forecast forecast) {
		forecasts.add(forecast);
		firePropertyChange("forecast", null, forecast);
	}

	public List<Forecast> getForecasts() {
		return Collections.unmodifiableList(forecasts);
	}

	public Forecast getForecast(String name) {
		return getByNameNonDeleted(forecasts, name);
	}

	public void addCredit(ICredit credit) {
		credits.add(credit);
		firePropertyChange("credit", null, credit);
	}

	public List<ICredit> getCredits() {
		return Collections.unmodifiableList(credits);
	}

	public ICredit getCredit(String name) {
		return getByNameNonDeleted(credits, name);
	}

	private <T extends INamedElement> T getByNameNonDeleted(Iterable<T> elements, final String name) {
		return Iterables.find(elements, new Predicate<T>() {
			@Override
			public boolean apply(T element) {
				if (! element.getName().equals(name)) {
					return false;
				}
				if (element instanceof IDeletable deletable) {
					return ! deletable.isDeleted();
				}
				return true;
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
		calendarEntries.clear();
		securityAlarms.clear();
		watchlistConfigurations.clear();
		savedTransactions = ImmutableList.of();
		securityCategoryMappings.clear();
	}

	public void reconfigureAfterDeserialization() {
		if (watchlistConfigurations == null) {
			watchlistConfigurations = new ArrayList<WatchlistConfiguration>();
		}
		if (calendarEntries == null) {
			calendarEntries = new ArrayList<CalendarEntry>();
		}
		if (securityAlarms == null) {
			securityAlarms = new ArrayList<>();
		}
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
		if (comparisons == null) {
			comparisons = new ArrayList<>();
			comparisons.add(SectorInput.init(this));
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
		if (stockSplitsPerSecurity == null) {
			stockSplitsPerSecurity = new ConcurrentHashMap<>();
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
			// Top level BasicData; check for transactions
			if (isCategoryUsed(categoryToRemove)) {
				throw new IllegalStateException("Can't remove category. BasicData is in use.");
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
			for (ITransaction transaction : account.getFlatTransactions()) {
				if (category.equals(transaction.getCategory())) {
					return true;
				}
			}
		}
		return false;
	}

	private void replaceAllCategories(Category categoryFrom, Category categoryTo) {
		for (Account account : accounts) {
			for (ITransaction transaction : account.getFlatTransactions()) {
				if (categoryFrom.equals(transaction.getCategory())) {
					transaction.setCategory(categoryTo);
				}
			}
		}
	}

	public List<Comparison> getComparisons() {
		return comparisons;
	}
	
	transient private Map<Security, ImmutableList<StockSplit>> stockSplitsPerSecurity = new ConcurrentHashMap<>();
	
	/**
	 * Returns all splits for the given security.
	 * The splits are ordered by date.
	 *
	 */
	public ImmutableList<StockSplit> getStockSplits(Security security) {
		ImmutableList<StockSplit> result = stockSplitsPerSecurity.get(security);
		if (result == null) {
			result = ImmutableSortedSet.copyOf(DAY_COMPARATOR,
					Iterables.filter(stockSplits, new Predicate<StockSplit>() {
						@Override
						public boolean apply(StockSplit stockSplit) {
							return stockSplit.getSecurity().equals(security);
						}
					}
				)).asList();
			stockSplitsPerSecurity.put(security, result);
		}
		return result;
	}

	public void addStockSplit(StockSplit stockSplit) {
		stockSplits.add(stockSplit);
		stockSplitsPerSecurity.remove(stockSplit.getSecurity());
	}

	public boolean removeStockSplit(StockSplit stockSplit) {
		boolean changed = stockSplits.remove(stockSplit);
		stockSplitsPerSecurity.remove(stockSplit.getSecurity());
		return changed;
	}

	public void setStockSplits(Security security, Set<StockSplit> splits) {
		for (Iterator<StockSplit> iter = stockSplits.iterator(); iter.hasNext();) {
			StockSplit stockSplit = iter.next();
			if (stockSplit.getSecurity().equals(security)) {
				iter.remove();
			}
		}
		stockSplits.addAll(splits);
		stockSplitsPerSecurity.clear();
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

	public ImmutableSet<WatchlistConfiguration> getWatchlsts() {
		return ImmutableSet.copyOf(watchlistConfigurations);
	}

	public void addWatchlist(WatchlistConfiguration watchlistConfiguration) {
		watchlistConfigurations.add(watchlistConfiguration);
		firePropertyChange("watchlist", null, watchlistConfiguration);
	}

	public boolean removeWatchlist(WatchlistConfiguration watchlistConfiguration) {
		boolean removed = watchlistConfigurations.remove(watchlistConfiguration);
		if (removed) {
			firePropertyChange("watchlist", watchlistConfiguration, null);
		}
		return removed;
	}

	/**
	 * Returns all calendar entry objects for the given security.
	 */
	public ImmutableSet<SecurityCalendarEntry> getCalendarEntries(final Security security) {
		return ImmutableSet.copyOf(
			Iterables.filter(Iterables.filter(calendarEntries, SecurityCalendarEntry.class), new Predicate<SecurityCalendarEntry>() {
				@Override
				public boolean apply(SecurityCalendarEntry calendarEntry) {
					if (calendarEntry.getSecurity().equals(security)) {
						return true;
					}
					return false;
				}
			}));
	}

	public ImmutableList<CalendarEntry> getCalendarEntries() {
		return ImmutableList.copyOf(calendarEntries);
	}

	public void addCalendarEntry(CalendarEntry calendarEntry) {
		calendarEntries.add(calendarEntry);
		firePropertyChange("calendarEntry", null, calendarEntry);
	}

	public boolean removeCalendarEntry(CalendarEntry calendarEntry) {
		boolean remove = calendarEntries.remove(calendarEntry);
		if (remove) {
			firePropertyChange("calendarEntry", calendarEntry, null);
		}
		return remove;
	}

	public ImmutableSet<SecurityAlarm> getSecurityAlarms(final Security security) {
		return ImmutableSet.copyOf(
			Iterables.filter(securityAlarms, new Predicate<SecurityAlarm>() {
				@Override
				public boolean apply(SecurityAlarm securityAlarm) {
					if (securityAlarm.getSecurity().equals(security)) {
						return true;
					}
					return false;
				}
			}));
	}

	public ImmutableList<SecurityAlarm> getSecurityAlarms() {
		return ImmutableList.copyOf(securityAlarms);
	}

	public void addSecurityAlarm(SecurityAlarm securityAlarm) {
		securityAlarms.add(securityAlarm);
		firePropertyChange("securityAlarm", null, securityAlarm);
	}

	public boolean removeSecurityAlarm(SecurityAlarm securityAlarm) {
		boolean remove = securityAlarms.remove(securityAlarm);
		if (remove) {
			firePropertyChange("securityAlarm", securityAlarm, null);
		}
		return remove;
	}

	public Inventory getFullInventory() {
		synchronized (this) {
			if (fullInventory == null) {
				Report report = new Report("temp");
				report.setAccounts(getAccounts());

				fullInventory = new Inventory(report, PriceProviderFactory.getInstance(), new AnalyzerFactory(), this);
			} else {
				// Day may have changed
				fullInventory.setDate(Day.today());
			}
			return fullInventory;
		}
	}
}
