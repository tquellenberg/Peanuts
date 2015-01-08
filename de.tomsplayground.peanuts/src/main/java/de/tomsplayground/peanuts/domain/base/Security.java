package de.tomsplayground.peanuts.domain.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.config.ConfigurableSupport;
import de.tomsplayground.peanuts.config.IConfigurable;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.domain.fundamental.FundamentalData;

@XStreamAlias("security")
public class Security extends ObservableModelObject implements INamedElement, IConfigurable {

	// Core
	private String name;

	// Optional
	private String ISIN;
	private String WKN;
	private String ticker;

	final private Map<String, String> displayConfiguration = new HashMap<String, String>();

	private List<FundamentalData> fundamentalDatas = new ArrayList<FundamentalData>();

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
	}

	public List<FundamentalData> getFundamentalDatas() {
		return fundamentalDatas;
	}

	public FundamentalData getCurrentFundamentalData() {
		if (fundamentalDatas.isEmpty()) {
			return null;
		}
		FundamentalData result = fundamentalDatas.get(0);
		for (FundamentalData data : fundamentalDatas) {
			if (data.getYear() > result.getYear()) {
				result = data;
			}
		}
		return result;
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
}
