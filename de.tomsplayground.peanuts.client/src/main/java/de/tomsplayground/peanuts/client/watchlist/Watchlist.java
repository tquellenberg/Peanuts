package de.tomsplayground.peanuts.client.watchlist;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.INamedElement;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.domain.process.StockSplit;

public class Watchlist extends ObservableModelObject implements INamedElement {

	private final PropertyChangeListener priceProviderChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			getPropertyChangeSupport().firePropertyChange(evt);
		}
	};
	
	private final String name;
	private final List<WatchEntry> entries = new ArrayList<WatchEntry>();

	public Watchlist(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	public List<WatchEntry> getEntries() {
		return Collections.unmodifiableList(entries);
	}
	
	public WatchEntry addEntry(Security security) {
		for (WatchEntry entry : entries) {
			if (entry.getSecurity().equals(security)) {
				return null;
			}
		}
		IPriceProvider priceProvider = PriceProviderFactory.getInstance().getPriceProvider(security);
		if (priceProvider instanceof ObservableModelObject) {
			ObservableModelObject ob = (ObservableModelObject) priceProvider;
			ob.addPropertyChangeListener(priceProviderChangeListener);
		}
		ImmutableList<StockSplit> stockSplits = Activator.getDefault().getAccountManager().getStockSplits(security);
		IPriceProvider adjustedPriceProvider = PriceProviderFactory.getInstance().getAdjustedPriceProvider(security, stockSplits);
		WatchEntry watchEntry = new WatchEntry(security, priceProvider, adjustedPriceProvider);
		entries.add(watchEntry);
		firePropertyChange("entries", null, watchEntry);
		return watchEntry;
	}
	
	public WatchEntry removeEntry(Security security) {
		for (Iterator<WatchEntry> iterator = entries.iterator(); iterator.hasNext();) {
			WatchEntry watchEntry = iterator.next();
			if (watchEntry.getSecurity().equals(security)) {
				iterator.remove();
				IPriceProvider priceProvider = watchEntry.getPriceProvider();
				if (priceProvider instanceof ObservableModelObject) {
					ObservableModelObject ob = (ObservableModelObject) priceProvider;
					ob.removePropertyChangeListener(priceProviderChangeListener);
				}
				firePropertyChange("entries", watchEntry, null);
				return watchEntry;
			}
		}
		return null;
	}
}
