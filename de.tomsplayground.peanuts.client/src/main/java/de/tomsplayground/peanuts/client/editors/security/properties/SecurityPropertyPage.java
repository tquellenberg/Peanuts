package de.tomsplayground.peanuts.client.editors.security.properties;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.fundamental.FundamentalDatas;

public class SecurityPropertyPage extends PropertyPage {

	public static final String MARKET_SCREENER_URL = "fourTrasdersUrl";

	public static final String OVERRIDE_EXISTING_PRICE_DATA = "OVERRIDE_EXISTING_PRICE_DATA";

	private Text name;
	private Text wkn;
	private Text isin;
	private Text ticker;
	private Text yahooSymbol;
	private Text morningstarSymbol;
	private Text fourTradersUrl;
	private Text overriddenAvgPE;

	private Button overridePriceDate;

	public SecurityPropertyPage() {
		noDefaultAndApplyButton();
	}

	@Override
	protected Control createContents(Composite parent) {
		IAdaptable adapter = getElement();
		Security security = adapter.getAdapter(Security.class);

		Composite composite = new Composite(parent,SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		name = createTextWithLabel(composite, "Name");
		name.setText(security.getName());

		wkn = createTextWithLabel(composite, "WKN");
		wkn.setText(StringUtils.defaultString(security.getWKN()));

		isin = createTextWithLabel(composite, "Isin");
		isin.setText(StringUtils.defaultString(security.getISIN()));

		ticker = createTextWithLabel(composite, "Ticker");
		ticker.setText(StringUtils.defaultString(security.getTicker()));

		yahooSymbol = createTextWithLabel(composite, "Yahoo-Symbol");
		yahooSymbol.setText(StringUtils.defaultString(security.getConfigurationValue(Security.CONFIG_KEY_YAHOO_SYMBOL)));

		morningstarSymbol = createTextWithLabel(composite, "Morningstar");
		morningstarSymbol.setText(StringUtils.defaultString(security.getMorningstarSymbol()));

		fourTradersUrl = createTextWithLabel(composite, "4-Traders Url");
		fourTradersUrl.setText(StringUtils.defaultString(security.getConfigurationValue(MARKET_SCREENER_URL)));

		overriddenAvgPE = createTextWithLabel(composite, "Overridden avg PE");
		overriddenAvgPE.setText(StringUtils.defaultString(security.getConfigurationValue(FundamentalDatas.OVERRIDDEN_AVG_PE)));

		Label label = new Label(composite, SWT.NONE);
		label.setText("Override existing price data");
		overridePriceDate = new Button(composite, SWT.CHECK);
		overridePriceDate.setSelection(Boolean.valueOf(security.getConfigurationValue(OVERRIDE_EXISTING_PRICE_DATA)).booleanValue());

		return composite;
	}

	private Text createTextWithLabel(Composite group, String labelText) {
		Label label = new Label(group, SWT.NONE);
		label.setText(labelText);
		Text t = new Text(group, SWT.BORDER);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		t.setLayoutData(gridData);
		return t;
	}

	@Override
	public boolean performOk() {
		IAdaptable adapter = getElement();
		Security security = adapter.getAdapter(Security.class);
		security.setName(name.getText());
		security.setWKN(wkn.getText());
		security.setISIN(isin.getText());
		security.setTicker(ticker.getText());
		security.setMorningstarSymbol(morningstarSymbol.getText());
		security.putConfigurationValue(MARKET_SCREENER_URL, fourTradersUrl.getText());
		security.putConfigurationValue(Security.CONFIG_KEY_YAHOO_SYMBOL, yahooSymbol.getText());
		security.putConfigurationValue(OVERRIDE_EXISTING_PRICE_DATA, Boolean.toString(overridePriceDate.getSelection()));
		String value = overriddenAvgPE.getText();
		if (StringUtils.isNotBlank(value)) {
			value = value.replace(',', '.');
		}
		security.putConfigurationValue(FundamentalDatas.OVERRIDDEN_AVG_PE, value);
		return super.performOk();
	}

}
