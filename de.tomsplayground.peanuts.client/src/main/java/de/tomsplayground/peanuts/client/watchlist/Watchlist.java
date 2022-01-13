package de.tomsplayground.peanuts.client.watchlist;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.INamedElement;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.domain.process.StockSplit;
import de.tomsplayground.peanuts.domain.watchlist.WatchlistConfiguration;

public class Watchlist extends ObservableModelObject implements INamedElement {

	private final static Logger log = LoggerFactory.getLogger(Watchlist.class);

	private final PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getSource() instanceof Security) {
				Security s = (Security) evt.getSource();
				getEntry(s).refreshCache();
			}
			if (evt.getSource() instanceof IPriceProvider) {
				IPriceProvider pp = (IPriceProvider) evt.getSource();
				getEntry(pp.getSecurity()).refreshCache();
			}
			getPropertyChangeSupport().firePropertyChange(evt);
		}
	};

	private WatchlistConfiguration configuration;

	private final List<WatchEntry> entries = new ArrayList<WatchEntry>();

	public Watchlist(WatchlistConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public String getName() {
		return configuration.getName();
	}

	public List<WatchEntry> getEntries() {
		return Collections.unmodifiableList(entries);
	}

	public Set<Security> getSecurities() {
		return Sets.newHashSet(Iterables.transform(entries, new Function<WatchEntry, Security>() {
			@Override
			public Security apply(WatchEntry input) {
				return input.getSecurity();
			}
		}));
	}

	public WatchEntry getEntry(Security security) {
		for (WatchEntry entry : entries) {
			if (entry.getSecurity().equals(security)) {
				return entry;
			}
		}
		return null;
	}

	public WatchEntry addEntry(Security security) {
		if (getEntry(security) != null) {
			return null;
		}
		security.addPropertyChangeListener(propertyChangeListener);
		IPriceProvider priceProvider = PriceProviderFactory.getInstance().getPriceProvider(security);
		if (priceProvider instanceof ObservableModelObject) {
			ObservableModelObject ob = (ObservableModelObject) priceProvider;
			ob.addPropertyChangeListener(propertyChangeListener);
		}
		ImmutableList<StockSplit> stockSplits = Activator.getDefault().getAccountManager().getStockSplits(security);
		IPriceProvider adjustedPriceProvider = PriceProviderFactory.getInstance().getSplitAdjustedPriceProvider(security, stockSplits);
		WatchEntry watchEntry = new WatchEntry(security, adjustedPriceProvider);
		entries.add(watchEntry);
		firePropertyChange("entries", null, watchEntry);
		return watchEntry;
	}

	public WatchEntry removeEntry(Security security) {
		log.info("Remove {} from watch list {}", security.getName(), configuration.getName());		
		for (Iterator<WatchEntry> iterator = entries.iterator(); iterator.hasNext();) {
			WatchEntry watchEntry = iterator.next();
			if (watchEntry.getSecurity().equals(security)) {
				iterator.remove();
				IPriceProvider priceProvider = PriceProviderFactory.getInstance().getPriceProvider(security);
				if (priceProvider instanceof ObservableModelObject) {
					ObservableModelObject ob = (ObservableModelObject) priceProvider;
					ob.removePropertyChangeListener(propertyChangeListener);
				}
				firePropertyChange("entries", watchEntry, null);
				security.removePropertyChangeListener(propertyChangeListener);
				log.info("Successfully removed");
				return watchEntry;
			}
		}
		log.error("Security {} not found in watch list {}", security.getName(), configuration.getName());
		return null;
	}

	public WatchlistConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(WatchlistConfiguration newConfiguration) {
		this.configuration = newConfiguration;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).append(getName()).build();
	}
}
