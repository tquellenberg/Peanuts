package de.tomsplayground.peanuts.client.watchlist;

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.domain.watchlist.WatchlistConfiguration;
import de.tomsplayground.peanuts.util.Day;

public class WatchlistManager extends ObservableModelObject {

	public static final String DEFAULT_WATCHLIST_NAME = "Default";

	private static WatchlistManager INSTANCE;

	private final List<WatchlistConfiguration> allWatchlists = new ArrayList<WatchlistConfiguration>();

	private Watchlist currentWatchlist;

	private Day performanceFrom;
	private Day performanceTo;

	private final PropertyChangeListener watchlistChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			getPropertyChangeSupport().firePropertyChange(evt);
		}
	};

	private WatchlistManager() {
		init();
	}

	public static synchronized WatchlistManager getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new WatchlistManager();
		}
		return INSTANCE;
	}

	private void init() {
		// Watch lists with configuration
		allWatchlists.addAll(Activator.getDefault().getAccountManager().getWatchlsts());

		// Manually configured watch lists from securities
//		ImmutableList<Security> allSecurities = Activator.getDefault().getAccountManager().getSecurities();
//		Set<String> manualWatchListName = allSecurities.parallelStream()
//			.flatMap(s -> getWatchlistNamesForSecurity(s).stream())
//			.collect(Collectors.toSet());
//		for (String watchListName : manualWatchListName) {
//			if (getWatchlistByName(watchListName) == null) {
//				WatchlistConfiguration watchlistConfiguration = new WatchlistConfiguration(watchListName);
//				Activator.getDefault().getAccountManager().addWatchlist(watchlistConfiguration);
//				allWatchlists.add(watchlistConfiguration);
//			}
//		}

		// Current watch list
		if (allWatchlists.isEmpty()) {
			addWatchlist(DEFAULT_WATCHLIST_NAME);
		}
		setCurrentWatchlist(getWatchlistNames().get(0));
	}

	public List<String> getWatchlistNames() {
		List<String> result = new ArrayList<String>();
		for (WatchlistConfiguration list : allWatchlists) {
			result.add(list.getName());
		}
		Collections.sort(result);
		return result;
	}

	private WatchlistConfiguration getWatchlistByName(String name) {
		for (WatchlistConfiguration list : allWatchlists) {
			if (list.getName().equals(name)) {
				return list;
			}
		}
		return null;
	}

	public WatchlistConfiguration addWatchlist(String name) {
		WatchlistConfiguration watchlist = getWatchlistByName(name);
		if (watchlist == null) {
			watchlist = new WatchlistConfiguration(name);
			allWatchlists.add(watchlist);
			Activator.getDefault().getAccountManager().addWatchlist(watchlist);
			firePropertyChange("allWatchlists", null, watchlist);
		}
		return watchlist;
	}

	private void refreshSecuritiesForWatchlist(Watchlist watchlist) {
		Set<Security> newSecurities = Sets.newHashSet();
		if (watchlist.getConfiguration().isManuallyConfigured()) {
			newSecurities.addAll(getManuallyWatchlistSecurities(watchlist.getName()));
		} else {
			newSecurities.addAll(watchlist.getConfiguration().getSecuritiesByConfiguration(Activator.getDefault().getAccountManager()));
		}

		Set<Security> currentSecurities = watchlist.getSecurities();
		for (Security s : Sets.filter(currentSecurities, not(in(newSecurities)))) {
			watchlist.removeEntry(s);
		}
		for (Security s : Sets.filter(newSecurities, not(in(currentSecurities)))) {
			watchlist.addEntry(s);
		}
	}

	public void setCurrentWatchlist(String name) {
		Watchlist oldCurrentWatchlist = currentWatchlist;
		if (oldCurrentWatchlist != null) {
			oldCurrentWatchlist.removePropertyChangeListener(watchlistChangeListener);
		}
		currentWatchlist = new Watchlist(getWatchlistByName(name));
		currentWatchlist.addPropertyChangeListener(watchlistChangeListener);

		firePropertyChange("currentWatchlist", oldCurrentWatchlist, currentWatchlist);
		refreshSecuritiesForWatchlist(currentWatchlist);
	}

	public WatchlistConfiguration getCurrentWatchlistConfiguration() {
		return new WatchlistConfiguration(currentWatchlist.getConfiguration());
	}

	public Watchlist getCurrentWatchlist() {
		return currentWatchlist;
	}

	public void deleteCurrentWatchlist() {
		if (currentWatchlist != null) {
			currentWatchlist.removePropertyChangeListener(watchlistChangeListener);
			WatchlistConfiguration watchlistConfiguration = currentWatchlist.getConfiguration();
			if (watchlistConfiguration.isManuallyConfigured()) {
				String name = watchlistConfiguration.getName();
				for (Security entry : getManuallyWatchlistSecurities(name)) {
					Set<String> list = new HashSet<String>(getWatchlistNamesForSecurity(entry));
					list.remove(name);
					entry.putConfigurationValue(SecurityWatchlistView.ID, StringUtils.join(list, ','));
				}
			}
			allWatchlists.remove(watchlistConfiguration);
			Activator.getDefault().getAccountManager().removeWatchlist(watchlistConfiguration);
			setCurrentWatchlist(allWatchlists.get(0).getName());
		}
	}

	public void updateCurrentWatchlist(WatchlistConfiguration newConfiguration) {
		WatchlistConfiguration currentConfiguration = currentWatchlist.getConfiguration();
		// Changed name
		if (! StringUtils.equals(newConfiguration.getName(), currentConfiguration.getName()) &&
			newConfiguration.isManuallyConfigured()) {
			String oldName = currentConfiguration.getName();
			String newName = newConfiguration.getName();
			for (Security entry : getManuallyWatchlistSecurities(oldName)) {
				Set<String> list = new HashSet<String>(getWatchlistNamesForSecurity(entry));
				list.remove(oldName);
				list.add(newName);
				entry.putConfigurationValue(SecurityWatchlistView.ID, StringUtils.join(list, ','));
			}
		}
		// Copy data
		currentConfiguration.setName(newConfiguration.getName());
		currentConfiguration.setType(newConfiguration.getType());
		currentConfiguration.setSorting(newConfiguration.getSorting());
		currentConfiguration.setFilters(newConfiguration.getFilters());
		// Refresh
		refreshSecuritiesForWatchlist(currentWatchlist);
		firePropertyChange("configuration", currentConfiguration, newConfiguration);
	}

	public BigDecimal getCustomPerformance(WatchEntry entry) {
		if (isCustomPerformanceRangeSet()) {
			return entry.getPerformance(getPerformanceFrom(), getPerformanceTo());
		}
		return BigDecimal.ZERO;
	}

	public boolean isCustomPerformanceRangeSet() {
		return performanceFrom != null && performanceTo != null &&
			performanceFrom.before(performanceTo);
	}

	public void setPerformanceFrom(Day performanceFrom) {
		this.performanceFrom = performanceFrom;
	}
	public Day getPerformanceFrom() {
		return performanceFrom;
	}

	public void setPerformanceTo(Day performanceTo) {
		this.performanceTo = performanceTo;
	}
	public Day getPerformanceTo() {
		return performanceTo;
	}

	private List<Security> getManuallyWatchlistSecurities(String watchlistName) {
		ImmutableList<Security> allSecurities = Activator.getDefault().getAccountManager().getSecurities();
		List<Security> securities = new ArrayList<>();
		for (Security security : allSecurities) {
			if (!security.isDeleted() && getWatchlistNamesForSecurity(security).contains(watchlistName)) {
				securities.add(security);
			}
		}
		return securities;
	}

	private List<String> getWatchlistNamesForSecurity(Security security) {
		String watchListsStr = security.getConfigurationValue(SecurityWatchlistView.ID);
		String[] watchLists = StringUtils.split(watchListsStr, ',');
		if (watchLists != null) {
			return new ArrayList<>(List.of(watchLists));
		}
		return new ArrayList<>();
	}

	public void addSecurityToCurrentWatchlist(Security security) {
		WatchlistConfiguration watchlist = currentWatchlist.getConfiguration();
		if (watchlist.isManuallyConfigured()) {
			List<String> list = getWatchlistNamesForSecurity(security);
			list.add(watchlist.getName());
			security.putConfigurationValue(SecurityWatchlistView.ID, StringUtils.join(list, ','));
		}
	}

	public void removeSecurityFromCurrentWatchlist(Security security) {
		WatchlistConfiguration watchlist = currentWatchlist.getConfiguration();
		if (watchlist.isManuallyConfigured()) {
			List<String> list = getWatchlistNamesForSecurity(security);
			list.remove(watchlist.getName());
			security.putConfigurationValue(SecurityWatchlistView.ID, StringUtils.join(list, ','));
		}
	}

}
