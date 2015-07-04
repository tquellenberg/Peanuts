package de.tomsplayground.peanuts.domain.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.config.ConfigurableSupport;
import de.tomsplayground.peanuts.config.IConfigurable;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.domain.currenncy.Currencies;
import de.tomsplayground.peanuts.domain.fundamental.FundamentalData;
import de.tomsplayground.peanuts.domain.note.Note;
import de.tomsplayground.util.Day;

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
		if (notes == null) {
			notes = new ArrayList<Note>();
		}
	}

	public List<FundamentalData> getFundamentalDatas() {
		return fundamentalDatas;
	}

	public FundamentalData getCurrentFundamentalData() {
		if (fundamentalDatas.isEmpty()) {
			return null;
		}
		final Day now = new Day();
		return Iterables.find(fundamentalDatas, new Predicate<FundamentalData>() {
			@Override
			public boolean apply(FundamentalData input) {
				int delta = now.delta(input.getFiscalEndDay());
				return delta > 0 && delta <= 360;
			}
		}, null);
	}

	public void setFundamentalDatas(List<FundamentalData> fundamentalDatas) {
		List<FundamentalData> old = this.fundamentalDatas;
		this.fundamentalDatas = new ArrayList<FundamentalData>(fundamentalDatas);
		firePropertyChange("fundamentalData", old, fundamentalDatas);
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

	public boolean remoteNote(Note note) {
		boolean remove = notes.remove(note);
		if (remove) {
			firePropertyChange("notes", note, null);
		}
		return remove;
	}

}
