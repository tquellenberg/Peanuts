package de.tomsplayground.peanuts.domain.base;

import static java.util.stream.Collectors.groupingBy;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.IPriceProviderFactory;
import de.tomsplayground.peanuts.domain.process.IStockSplitProvider;
import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.domain.process.StockSplit;
import de.tomsplayground.peanuts.domain.reporting.investment.AnalyzerFactory;
import de.tomsplayground.peanuts.util.Day;

public class Inventory extends ObservableModelObject {

	private final ConcurrentHashMap<Security, InventoryEntry> entryMap = new ConcurrentHashMap<Security, InventoryEntry>();
	private final AnalyzerFactory analizerFactory;
	private final IPriceProviderFactory priceProviderFactory;
	private final ITransactionProvider account;
	private ImmutableList<ITransaction> allFlatTransactions;
	
	private Day day;
	
	private final Set<ObservableModelObject> registeredPriceProvider = new CopyOnWriteArraySet<>();

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
			if (evt.getSource() instanceof Account) {
				System.out.println("Inventory.transactionChangeListener" + Inventory.this + " " + evt);
				Inventory.this.allFlatTransactions = account.getFlatTransactions();
				synchronized (entryMap) {
					fullRebuild();
				}
				getPropertyChangeSupport().firePropertyChange("entries", 0, 1);
			} else {
				// Ignore detailed transaction events
			}
		}
	};
	
	private PropertyChangeListener securityChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			getPropertyChangeSupport().firePropertyChange(evt);
		}
	};
	private IStockSplitProvider stockSplitProvider;

	public Inventory(ITransactionProvider account, IPriceProviderFactory priceProviderFactory, AnalyzerFactory analizerFactory,
			IStockSplitProvider stockSplitProvider) {
		this.account = account;
		this.priceProviderFactory = priceProviderFactory;
		this.stockSplitProvider = stockSplitProvider;
		this.day = Day.today();
		this.analizerFactory = analizerFactory;
		this.allFlatTransactions = account.getFlatTransactions();
		buildInventoryEntries();
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
		for (Security security : entryMap.keySet()) {
			security.removePropertyChangeListener(securityChangeListener);
		}
	}

	private void fullRebuild() {
		for (Security security : entryMap.keySet()) {
			security.removePropertyChangeListener(securityChangeListener);
		}
		entryMap.clear();
		buildInventoryEntries();
	}
	
	public void setDate(Day day) {
		if (day.equals(this.day)) {
			return;
		}
//		if (day.after(this.day)) {
//			Day startDay = this.day.addDays(1);
//			synchronized (entryMap) {
//				this.day = day;
//				ImmutableList<ITransaction> transactionsByDate = account.getFlatTransactionsByDate(startDay, day);
//				if (transactionsByDate.isEmpty()) {
//					return;
//				}
//				setTransactions(transactionsByDate);
//			}
//		} else {
			synchronized (entryMap) {
				this.day = day;
				fullRebuild();
			}
//		}
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
	
	private void buildInventoryEntries() {
		ImmutableList<ITransaction> transactions = TransactionProviderUtil.getTransactionsByDate(allFlatTransactions, null, day);
		if (transactions.isEmpty()) {
			return;
		}
		List<InvestmentTransaction> invests = new ArrayList<>();
		for (ITransaction transaction : transactions) {
			if (transaction instanceof InvestmentTransaction) {
				invests.add((InvestmentTransaction) transaction);
			}
		}
		invests.stream()
			.collect(groupingBy(InvestmentTransaction::getSecurity))
			.entrySet()
			.parallelStream()
			.forEach(e -> buildInventoryEntriesForSecurity(e.getKey(), e.getValue()));
	}

	private void buildInventoryEntriesForSecurity(Security security, List<InvestmentTransaction> invTrans) {
		InventoryEntry inventoryEntry = getInventoryEntry(security);
		for (InvestmentTransaction t : invTrans) {
			inventoryEntry.add(t, analizerFactory);
		}
	}

	public InventoryEntry getInventoryEntry(Security security) {
		if ( ! entryMap.containsKey(security)) {
			// New security
			IPriceProvider priceprovider = null;
			if (priceProviderFactory != null) {
				priceprovider = priceProviderFactory.getPriceProvider(security);
				if (priceprovider instanceof ObservableModelObject) {
					ObservableModelObject ob = (ObservableModelObject) priceprovider;
					ob.addPropertyChangeListener(priceProviderChangeListener);
					registeredPriceProvider.add(ob);
				}
			}
			security.addPropertyChangeListener(securityChangeListener);
			ImmutableList<StockSplit> stockSplits = ImmutableList.of();
			if (stockSplitProvider != null) {
				stockSplits = stockSplitProvider.getStockSplits(security);
			}
			entryMap.putIfAbsent(security, new InventoryEntry(security, day, priceprovider, stockSplits));
		}
		return entryMap.get(security);
	}

	public BigDecimal getUnrealizedGainings() {
		synchronized (entryMap) {
			return entryMap.values().parallelStream()
					.map(e -> e.getMarketValue().subtract(e.getInvestedAmount()))
					.reduce(BigDecimal.ZERO, BigDecimal::add);
		}
	}

	public BigDecimal getMarketValue() {
		synchronized (entryMap) {
			return entryMap.values().parallelStream()
				.map(e -> e.getMarketValue())
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		}
	}

	public BigDecimal getDayChange() {
		synchronized (entryMap) {
			return entryMap.values().parallelStream()
					.map(e -> e.getDayChange())
					.reduce(BigDecimal.ZERO, BigDecimal::add);
		}
	}

}
