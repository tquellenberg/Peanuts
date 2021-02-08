package de.tomsplayground.peanuts.domain.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.google.common.collect.ImmutableList;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.config.ConfigurableSupport;
import de.tomsplayground.peanuts.config.IConfigurable;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.domain.currenncy.Currencies;
import de.tomsplayground.peanuts.domain.dividend.Dividend;
import de.tomsplayground.peanuts.domain.fundamental.FundamentalData;
import de.tomsplayground.peanuts.domain.fundamental.FundamentalDatas;
import de.tomsplayground.peanuts.domain.note.Note;

@XStreamAlias("security")
public class Security extends ObservableModelObject implements INamedElement, IConfigurable, IDeletable {

	// Core
	private String name;

	// Optional
	private String ISIN;
	private String WKN;
	private String ticker;
	private String morningstarSymbol;
	private String currency;
	// this prices for this security are exchange rates for this currency
	private String exchangeCurrency;
	private boolean deleted;

	final private Map<String, String> displayConfiguration = new HashMap<String, String>();

	private List<FundamentalData> fundamentalDatas = new ArrayList<FundamentalData>();

	private List<Note> notes = new ArrayList<Note>();

	private List<Dividend> dividends = new ArrayList<Dividend>();

	public Security(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("name", name).toString();
	}

	public String getISIN() {
		return ISIN;
	}

	@Override
	public String getName() {
		return name;
	}

	public String getWKN() {
		return WKN;
	}

	public String getTicker() {
		return ticker;
	}

	public void setTicker(String ticker) {
		String oldValue = this.ticker;
		this.ticker = ticker;
		firePropertyChange("ticker", oldValue, ticker);
	}

	public void setName(String name) {
		String oldValue = this.name;
		this.name = name;
		firePropertyChange("name", oldValue, name);
	}

	public void setISIN(String isin) {
		String oldValue = this.ISIN;
		ISIN = isin;
		firePropertyChange("isin", oldValue, isin);
	}

	public void setWKN(String wkn) {
		String oldValue = this.WKN;
		WKN = wkn;
		firePropertyChange("wkn", oldValue, wkn);
	}

	public void reconfigureAfterDeserialization(@SuppressWarnings("unused")AccountManager accountManager) {
		if (fundamentalDatas == null) {
			fundamentalDatas = new ArrayList<FundamentalData>();
		}
		fixCurrencies(fundamentalDatas);
		if (notes == null) {
			notes = new ArrayList<Note>();
		}
		if (dividends == null) {
			dividends = new ArrayList<>();
		}
		for (Dividend dividend : dividends) {
			dividend.setSecurity(this);
		}
	}

	public FundamentalDatas getFundamentalDatas() {
		return new FundamentalDatas(fundamentalDatas, this);
	}

	public void setFundamentalDatas(List<FundamentalData> fundamentalDatas) {
		checkCurrencies(fundamentalDatas);
		List<FundamentalData> old = this.fundamentalDatas;
		this.fundamentalDatas = new ArrayList<FundamentalData>(fundamentalDatas);
		firePropertyChange("fundamentalData", old, fundamentalDatas);
	}

	private void fixCurrencies(List<FundamentalData> fundamentalDatas) {
		if (fundamentalDatas.isEmpty()) {
			return;
		}
		Currency dataCurrency = fundamentalDatas.get(0).getCurrency();
		for (FundamentalData fundamentalData : fundamentalDatas) {
			if (! fundamentalData.getCurrency().equals(dataCurrency)) {
				fundamentalData.setCurrency(dataCurrency);
			}
		}
	}

	private void checkCurrencies(List<FundamentalData> fundamentalDatas) {
		if (fundamentalDatas.isEmpty()) {
			return;
		}
		Currency dataCurrency = fundamentalDatas.get(0).getCurrency();
		for (FundamentalData fundamentalData : fundamentalDatas) {
			if (! fundamentalData.getCurrency().equals(dataCurrency)) {
				throw new IllegalArgumentException("All fundamental data must use the same currency.");
			}
		}
	}

	private transient ConfigurableSupport configurableSupport;

	private ConfigurableSupport getConfigurableSupport() {
		if (configurableSupport == null) {
			configurableSupport = new ConfigurableSupport(displayConfiguration, getPropertyChangeSupport());
		}
		return configurableSupport;
	}

	@Override
	public String getConfigurationValue(String key) {
		return getConfigurableSupport().getConfigurationValue(key);
	}

	@Override
	public void putConfigurationValue(String key, String value) {
		getConfigurableSupport().putConfigurationValue(key, value);
	}

	public Currency getCurrency() {
		if (StringUtils.isBlank(currency)) {
			return Currencies.getInstance().getDefaultCurrency();
		}
		return Currency.getInstance(currency);
	}

	public void setCurreny(Currency curreny) {
		this.currency = curreny.getCurrencyCode();
	}

	public Currency getExchangeCurrency() {
		if (StringUtils.isBlank(exchangeCurrency)) {
			return null;
		}
		return Currency.getInstance(exchangeCurrency);
	}

	public void setExchangeCurreny(Currency curreny) {
		if (curreny == null) {
			this.exchangeCurrency = "";
		} else {
			this.exchangeCurrency = curreny.getCurrencyCode();
		}
	}

	@Override
	public boolean isDeleted() {
		return deleted;
	}

	@Override
	public void setDeleted(boolean deleted) {
		if (this.deleted == deleted) {
			return;
		}
		this.deleted = deleted;
		firePropertyChange("deleted", Boolean.valueOf(!deleted), Boolean.valueOf(deleted));
	}

	public String getMorningstarSymbol() {
		if (morningstarSymbol == null) {
			return "";
		}
		return morningstarSymbol;
	}

	public void setMorningstarSymbol(String morningstarSymbol) {
		this.morningstarSymbol = morningstarSymbol;
	}

	public ImmutableList<Note> getNotes() {
		return ImmutableList.copyOf(notes);
	}

	public void addNote(Note note) {
		notes.add(note);
		Collections.sort(notes);
		firePropertyChange("notes", null, note);
	}

	public boolean removeNote(Note note) {
		boolean remove = notes.remove(note);
		if (remove) {
			firePropertyChange("notes", note, null);
		}
		return remove;
	}

	public ImmutableList<Dividend> getDividends() {
		return ImmutableList.copyOf(dividends);
	}

	public void updateDividends(Collection<Dividend> updatedDividends) {
		if (CollectionUtils.isEqualCollection(dividends, updatedDividends)) {
			return;
		}
		updatedDividends.forEach(d -> d.setSecurity(this));
		dividends.clear();
		dividends.addAll(updatedDividends);
		Collections.sort(dividends);
		firePropertyChange("dividends", null, null);
	}

	public void addDividend(Dividend dividend) {
		dividend.setSecurity(this);
		dividends.add(dividend);
		Collections.sort(dividends);
		firePropertyChange("dividends", null, dividend);
	}

	public boolean removeDividend(Dividend dividend) {
		boolean remove = dividends.remove(dividend);
		if (remove) {
			firePropertyChange("dividends", dividend, null);
		}
		return remove;
	}

}
