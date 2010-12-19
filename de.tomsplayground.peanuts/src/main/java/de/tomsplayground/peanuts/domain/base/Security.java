package de.tomsplayground.peanuts.domain.base;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.config.IConfigurable;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;

@XStreamAlias("security")
public class Security extends ObservableModelObject implements INamedElement, IConfigurable {

	// Core
	private String name;

	// Optional
	private String ISIN;
	private String WKN;
	private String ticker;
	
	final private Map<String, String> displayConfiguration = new ConcurrentHashMap<String, String>();

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
		// Not used
	}

	@Override
	public Map<String, String> getDisplayConfiguration() {
		return displayConfiguration;
	}

}
