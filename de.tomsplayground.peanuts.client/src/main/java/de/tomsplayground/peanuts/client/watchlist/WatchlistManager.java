package de.tomsplayground.peanuts.client.watchlist;

import static com.google.common.base.Predicates.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.domain.watchlist.WatchlistConfiguration;
import de.tomsplayground.util.Day;

public class WatchlistManager extends ObservableModelObject {

	private static WatchlistManager INSTANCE = new WatchlistManager();

	private final List<Watchlist> watchlists = new ArrayList<Watchlist>();
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
		// private
	}

	public static WatchlistManager getInstance() {
		return INSTANCE;
	}

	public void init() {
		// Manually added securities
		for (Security security : Activator.getDefault().getAccountManager().getSecurities()) {
			for (String watchlistName : getWatchlistNamesForSecurity(security)) {
				Watchlist watchlist = getWatchlistByName(watchlistName);
				if (watchlist == null) {
					watchlist = addWatchlist(watchlistName);
				}
				watchlist.addEntry(security);
			}
		}
		// Configuration based securities
		for (WatchlistConfiguration config : Activator.getDefault().getAccountManager().getWatchlsts()) {
			Watchlist watchlist = getWatchlistByName(config.getName());
			if (watchlist == null) {
				watchlist = addWatchlist(config.getName());
			}
			for (Security s : getSecuritiesByConfiguration(watchlist.getConfiguration())) {
				watchlist.addEntry(s);
			}
		}
	}

	private void refreshSecuritiesForWatchlist(Watchlist watchlist) {
		Set<Security> currentSecurities = watchlist.getSecurities();
		Set<Security> newSecurities = Sets.newHashSet();
		for (Security security : Activator.getDefault().getAccountManager().getSecurities()) {
			if (getWatchlistNamesForSecurity(security).contains(watchlist.getName())) {
				newSecurities.add(security);
			}
		}
		newSecurities.addAll(getSecuritiesByConfiguration(watchlist.getConfiguration()));

		for (Security s : Sets.filter(currentSecurities, not(in(newSecurities)))) {
			watchlist.removeEntry(s);
		}
		for (Security s : Sets.filter(newSecurities, not(in(currentSecurities)))) {
			watchlist.addEntry(s);
		}
	}

	private List<Security> getSecuritiesByConfiguration(final WatchlistConfiguration configuration) {
		Collection<Security> result = null;
		switch (configuration.getType()) {
			case MANUAL:
				return Lists.newArrayList();
			case ALL_SECURITIES:
				result = Activator.getDefault().getAccountManager().getSecurities();
				break;
			case MY_SECURITIES:
				Inventory inventory = Activator.getDefault().getAccountManager().getFullInventory();
				result = inventory.getSecurities();
		}
		return Lists.newArrayList(Iterables.filter(result, new Predicate<Security>() {
			@Override
			public boolean apply(Security security) {
				return configuration.accept(security, Activator.getDefault().getAccountManager());
			}
		}));
	}

	public BigDecimal getCustomPerformance(WatchEntry entry) {
		if (isCustomPerformanceRangeSet()) {
			return entry.getPerformance(getPerformanceFrom(), getPerformanceTo());
		}
		return BigDecimal.ZERO;
	}

	void setCurrentWatchlist(String name) {
		Watchlist oldCurrentWatchlist = currentWatchlist;
		currentWatchlist = getWatchlistByName(name);
		firePropertyChange("currentWatchlist", oldCurrentWatchlist, currentWatchlist);
		refreshSecuritiesForWatchlist(currentWatchlist);
	}

	public Watchlist getCurrentWatchlist() {
		if (watchlists.isEmpty()) {
			addWatchlist("Default");
			currentWatchlist = watchlists.get(0);
		}
		return currentWatchlist;
	}

	public WatchlistConfiguration getWatchlistConfiguration(final String name) {
		ImmutableSet<WatchlistConfiguration> watchlsts = Activator.getDefault().getAccountManager().getWatchlsts();
		Optional<WatchlistConfiguration> optional = Iterables.tryFind(watchlsts, new Predicate<WatchlistConfiguration>() {
			@Override
			public boolean apply(WatchlistConfiguration input) {
				return input.getName().equals(name);
			}
		});
		WatchlistConfiguration watchlistConfiguration;
		if (optional.isPresent()) {
			watchlistConfiguration = optional.get();
		} else {
			watchlistConfiguration = new WatchlistConfiguration(name);
			Activator.getDefault().getAccountManager().addWatchlist(watchlistConfiguration);
		}
		return watchlistConfiguration;
	}

	public void update(WatchlistConfiguration watchlistConfiguration) {
		String oldName = currentWatchlist.getName();
		String newName = watchlistConfiguration.getName();
		if (! StringUtils.equals(newName, oldName)) {
			for (WatchEntry entry : currentWatchlist.getEntries()) {
				Set<String> list = new HashSet<String>(getWatchlistNamesForSecurity(entry.getSecurity()));
				list.remove(oldName);
				list.add(newName);
				entry.getSecurity().putConfigurationValue(SecurityWatchlistView.ID, StringUtils.join(list, ','));
			}
			currentWatchlist.setName(newName);
		}
		refreshSecuritiesForWatchlist(currentWatchlist);
	}

	public List<String> getWatchlistNames() {
		List<String> result = new ArrayList<String>();
		for (Watchlist list : watchlists) {
			result.add(list.getName());
		}
		return result;
	}

	public Watchlist getWatchlistByName(String name) {
		for (Watchlist list : watchlists) {
			if (list.getName().equals(name)) {
				return list;
			}
		}
		return null;
	}

	public Watchlist addWatchlist(String name) {
		Watchlist newWatchlist = new Watchlist(getWatchlistConfiguration(name));
		watchlists.add(newWatchlist);
		newWatchlist.addPropertyChangeListener(watchlistChangeListener);
		firePropertyChange("watchlists", null, newWatchlist);
		return newWatchlist;
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

	public List<String> getWatchlistNamesForSecurity(Security security) {
		String watchListsStr = security.getConfigurationValue(SecurityWatchlistView.ID);
		String[] watchLists = StringUtils.split(watchListsStr, ',');
		if (watchLists != null) {
			return Arrays.asList(watchLists);
		}
		return Collections.emptyList();
	}

	public void addSecurityToWatchlist(Security security, Watchlist watchlist) {
		Set<String> list = new HashSet<String>(getWatchlistNamesForSecurity(security));
		list.add(watchlist.getName());
		security.putConfigurationValue(SecurityWatchlistView.ID, StringUtils.join(list, ','));
	}

	public void removeSecurityFromWatchlist(Security security, Watchlist watchlist) {
		Set<String> list = new HashSet<String>(getWatchlistNamesForSecurity(security));
		list.remove(watchlist.getName());
		security.putConfigurationValue(SecurityWatchlistView.ID, StringUtils.join(list, ','));
	}

}
