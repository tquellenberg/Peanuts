package de.tomsplayground.peanuts.client.watchlist;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;

public class WatchlistManager extends ObservableModelObject {

	private static WatchlistManager INSTANCE = new WatchlistManager();
	
	List<Watchlist> watchlists = new ArrayList<Watchlist>();
	Watchlist currentWatchlist;
	
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
	
	void setCurrentWatchlist(String name) {
		Watchlist oldCurrentWatchlist = currentWatchlist;
		currentWatchlist = getWatchlist(name);
		firePropertyChange("currentWatchlist", oldCurrentWatchlist, currentWatchlist);
	}
	
	public Watchlist getCurrentWatchlist() {
		if (watchlists.isEmpty()) {
			addWatchlist("Default");
			currentWatchlist = watchlists.get(0);
		}
		return currentWatchlist;
	}
	
	public List<String> getWatchlistNames() {
		List<String> result = new ArrayList<String>();
		for (Watchlist list : watchlists) {
			result.add(list.getName());
		}
		return result;
	}
	
	public Watchlist getWatchlist(String name) {
		for (Watchlist list : watchlists) {
			if (list.getName().equals(name)) {
				return list;
			}
		}
		return null;
	}
	
	public Watchlist addWatchlist(String name) {
		Watchlist newWatchlist = new Watchlist(name);
		watchlists.add(newWatchlist);
		newWatchlist.addPropertyChangeListener(watchlistChangeListener);
		firePropertyChange("watchlists", null, newWatchlist);
		return newWatchlist;
	}
	
}
