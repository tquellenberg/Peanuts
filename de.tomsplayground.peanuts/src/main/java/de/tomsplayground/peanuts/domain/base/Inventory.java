package de.tomsplayground.peanuts.domain.base;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.IPriceProviderFactory;
import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction.Type;
import de.tomsplayground.peanuts.domain.reporting.investment.AnalyzedInvestmentTransaction;
import de.tomsplayground.peanuts.domain.reporting.investment.AnalyzerFactory;
import de.tomsplayground.peanuts.domain.reporting.investment.IAnalyzer;
import de.tomsplayground.util.Day;

public class Inventory extends ObservableModelObject {

	private final Map<Security, InventoryEntry> entryMap = new HashMap<Security, InventoryEntry>();
	private final AnalyzerFactory analizerFactory;
	private final IPriceProviderFactory priceProviderFactory;
	private final ITransactionProvider account;
	private Day day;

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
		if (day == null)
			this.day = new Day();
		else
			this.day = day;
		this.analizerFactory = analizerFactory;
		setTransactions(account.getTransactionsByDate(null, day));
		if (account instanceof ObservableModelObject) {
			ObservableModelObject a = (ObservableModelObject) account;
			a.addPropertyChangeListener(transactionChangeListener);
		}
	}
	
	public void setDate(Day day) {
		if (day.after(this.day)) {
			Day startDay = this.day.addDays(1);
			synchronized (entryMap) {
				this.day = day;
				for (InventoryEntry entry : entryMap.values()) {
					entry.setDate(day);
				}
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
	
	public Set<Security> getSecurities() {
		synchronized (entryMap) {
			return new HashSet<Security>(entryMap.keySet());
		}
	}

	public Collection<InventoryEntry> getEntries() {
		synchronized (entryMap) {
			return new HashSet<InventoryEntry>(entryMap.values());
		}
	}

	private void setTransactions(List<? extends ITransaction> transactions) {
		for (ITransaction transaction : transactions) {		
			if (transaction instanceof InvestmentTransaction) {
				InvestmentTransaction invTrans = (InvestmentTransaction) transaction;
				addTransaction(invTrans);
			} else {
				for (ITransaction t2 : transaction.getSplits()) {
					if (t2 instanceof InvestmentTransaction) {
						InvestmentTransaction invTrans = (InvestmentTransaction) t2;
						addTransaction(invTrans);
					}					
				}
			}
		}
	}

	private void addTransaction(InvestmentTransaction invTrans) {
		InventoryEntry inventoryEntry = getInventoryEntry(invTrans.getSecurity());
		Type type = invTrans.getType();
		if (type == InvestmentTransaction.Type.BUY ||
			type == InvestmentTransaction.Type.SELL) {
			if (analizerFactory != null) {
				List<InvestmentTransaction> transations = new ArrayList<InvestmentTransaction>(inventoryEntry.getTransations());
				transations.add(invTrans);
				IAnalyzer analizer = analizerFactory.getAnalizer(transations);
				List<AnalyzedInvestmentTransaction> analyzedTransactions = analizer.getAnalyzedTransactions();
				invTrans = analyzedTransactions.get(analyzedTransactions.size() - 1);
			}
			inventoryEntry.add(invTrans);
		} else {
			inventoryEntry.add(invTrans);
		}
	}
	
	private InventoryEntry getInventoryEntry(Security security) {
		InventoryEntry inventoryEntry;
		if ( !entryMap.containsKey(security)) {
			IPriceProvider priceprovider = null;
			if (priceProviderFactory != null) {
				priceprovider = priceProviderFactory.getPriceProvider(security);
				if (priceprovider instanceof ObservableModelObject) {
					ObservableModelObject ob = (ObservableModelObject) priceprovider;
					ob.addPropertyChangeListener(priceProviderChangeListener);
				}
			}
			inventoryEntry = new InventoryEntry(security, priceprovider, day);
			entryMap.put(security, inventoryEntry);
		}
		return entryMap.get(security);		
	}

	public BigDecimal getGainings() {
		BigDecimal gainings = BigDecimal.ZERO;
		for (InventoryEntry entry : getEntries()) {
			gainings = gainings.add(entry.getGaining());
		}
		return gainings;
	}

	public BigDecimal getMarketValue() {
		BigDecimal sum = BigDecimal.ZERO;
		synchronized (entryMap) {
			for (InventoryEntry entry : entryMap.values()) {
				sum = sum.add(entry.getMarketValue());
			}
		}
		return sum;
	}
	
}
