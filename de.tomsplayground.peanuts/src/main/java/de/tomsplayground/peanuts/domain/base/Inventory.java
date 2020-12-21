package de.tomsplayground.peanuts.domain.base;

import static java.util.stream.Collectors.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.IPriceProviderFactory;
import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction.Type;
import de.tomsplayground.peanuts.domain.reporting.investment.AnalyzerFactory;
import de.tomsplayground.peanuts.domain.reporting.investment.IAnalyzer;
import de.tomsplayground.util.Day;

public class Inventory extends ObservableModelObject {

	private final ConcurrentHashMap<Security, InventoryEntry> entryMap = new ConcurrentHashMap<Security, InventoryEntry>();
	private final AnalyzerFactory analizerFactory;
	private final IPriceProviderFactory priceProviderFactory;
	private final ITransactionProvider account;
	private Day day;

	private final List<ObservableModelObject> registeredPriceProvider = new CopyOnWriteArrayList<>();

	private final PropertyChangeListener priceProviderChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getSource() instanceof IPriceProvider) {
				IPriceProvider p = (IPriceProvider) evt.getSource();
				for (InventoryEntry entry : getEntries()) {
					if (entry.getPriceprovider() == p) {
						firePropertyChange("entry", null, entry);
						break;
					}
				}
			}
		}
	};
	private final PropertyChangeListener transactionChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			synchronized (entryMap) {
				entryMap.clear();
				setTransactions(account.getTransactionsByDate(null, day));
			}
			getPropertyChangeSupport().firePropertyChange("entries", 0, 1);
		}
	};

	public Inventory(Account account) {
		this(account, null, null, null);
	}

	public Inventory(ITransactionProvider account, IPriceProviderFactory priceProviderFactory) {
		this(account, priceProviderFactory, null, null);
	}

	public Inventory(ITransactionProvider account, IPriceProviderFactory priceProviderFactory, Day day, AnalyzerFactory analizerFactory) {
		this.account = account;
		this.priceProviderFactory = priceProviderFactory;
		if (day == null) {
			this.day = new Day();
		} else {
			this.day = day;
		}
		this.analizerFactory = analizerFactory;
		setTransactions(account.getTransactionsByDate(null, day));
		if (account instanceof ObservableModelObject) {
			ObservableModelObject a = (ObservableModelObject) account;
			a.addPropertyChangeListener(transactionChangeListener);
		}
	}

	public void dispose() {
		if (account instanceof ObservableModelObject) {
			ObservableModelObject a = (ObservableModelObject) account;
			a.removePropertyChangeListener(transactionChangeListener);
		}
		for (ObservableModelObject observableModelObject : registeredPriceProvider) {
			observableModelObject.removePropertyChangeListener(priceProviderChangeListener);
		}
	}

	public void setDate(Day day) {
		if (day.after(this.day)) {
			Day startDay = this.day.addDays(1);
			synchronized (entryMap) {
				this.day = day;
				setTransactions(account.getTransactionsByDate(startDay, day));
			}
		} else {
			synchronized (entryMap) {
				this.day = day;
				entryMap.clear();
				setTransactions(account.getTransactionsByDate(null, day));
			}
		}
	}

	public Day getDay() {
		return day;
	}

	public ImmutableSet<Security> getSecurities() {
		synchronized (entryMap) {
			return ImmutableSet.copyOf(entryMap.keySet());
		}
	}

	public ImmutableSet<InventoryEntry> getEntries() {
		synchronized (entryMap) {
			return ImmutableSet.copyOf(entryMap.values());
		}
	}

	public InventoryEntry getEntry(Security security) {
		synchronized (entryMap) {
			return entryMap.get(security);
		}
	}

	private void setTransactions(ImmutableList<? extends ITransaction> transactions) {
		if (transactions.isEmpty()) {
			return;
		}
		List<InvestmentTransaction> invests = new ArrayList<>();
		for (ITransaction transaction : transactions) {
			if (transaction instanceof InvestmentTransaction) {
				invests.add((InvestmentTransaction) transaction);
			} else {
				for (ITransaction t2 : transaction.getSplits()) {
					if (t2 instanceof InvestmentTransaction) {
						invests.add((InvestmentTransaction) t2);
					}
				}
			}
		}
		invests.stream()
			.collect(groupingBy(InvestmentTransaction::getSecurity))
			.entrySet()
			.parallelStream()
			.forEach(e -> addTransactions(e.getKey(), e.getValue()));
	}

	private void addTransactions(Security security, List<InvestmentTransaction> invTrans) {
		InventoryEntry inventoryEntry = getInventoryEntry(security);
		for (InvestmentTransaction t : invTrans) {
			Type type = t.getType();
			if (type == InvestmentTransaction.Type.BUY ||
				type == InvestmentTransaction.Type.SELL) {
				if (analizerFactory != null) {
					Iterable<InvestmentTransaction> transations = Iterables.concat(
							inventoryEntry.getTransactions(),
							ImmutableList.of(t));
					IAnalyzer analizer = analizerFactory.getAnalizer();
					t = Iterables.getLast(analizer.getAnalyzedTransactions(transations));
				}
				inventoryEntry.add(t);
			} else {
				inventoryEntry.add(t);
			}
		}
	}

	public InventoryEntry getInventoryEntry(Security security) {
		if ( !entryMap.containsKey(security)) {
			IPriceProvider priceprovider = null;
			if (priceProviderFactory != null) {
				priceprovider = priceProviderFactory.getPriceProvider(security);
				if (priceprovider instanceof ObservableModelObject) {
					ObservableModelObject ob = (ObservableModelObject) priceprovider;
					ob.addPropertyChangeListener(priceProviderChangeListener);
					registeredPriceProvider.add(ob);
				}
			}
			entryMap.putIfAbsent(security, new InventoryEntry(security, priceprovider));
		}
		return entryMap.get(security);
	}

	public BigDecimal getUnrealizedGainings() {
		synchronized (entryMap) {
			return entryMap.values().parallelStream()
					.map(e -> e.getMarketValue(day).subtract(e.getInvestedAmount()))
					.reduce(BigDecimal.ZERO, BigDecimal::add);
		}
	}

	public BigDecimal getMarketValue() {
		synchronized (entryMap) {
			return entryMap.values().parallelStream()
				.map(e -> e.getMarketValue(day))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		}
	}

	public BigDecimal getDayChange() {
		Day fromDay = day.addDays(-1);
		synchronized (entryMap) {
			return entryMap.values().parallelStream()
					.map(e -> e.getChange(fromDay, day))
					.reduce(BigDecimal.ZERO, BigDecimal::add);
		}
	}

}
