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

public class SecurityPropertyPage extends PropertyPage {

	public static final String OVERRIDE_EXISTING_PRICE_DATA = "OVERRIDE_EXISTING_PRICE_DATA";

	private Text name;
	private Text wkn;
	private Text isin;
	private Text ticker;
	private Text morningstarSymbol;
	private Text fourTradersUrl;
	private Button overridePriceDate;

	public SecurityPropertyPage() {
		noDefaultAndApplyButton();
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent,SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		name = createTextWithLabel(composite, "Name");
		wkn = createTextWithLabel(composite, "WKN");
		isin = createTextWithLabel(composite, "Isin");
		ticker = createTextWithLabel(composite, "Ticker");
		morningstarSymbol = createTextWithLabel(composite, "Morningstar");
		fourTradersUrl = createTextWithLabel(composite, "4-Traders Url");

		Label label = new Label(composite, SWT.NONE);
		label.setText("Override existing price data");
		overridePriceDate = new Button(composite, SWT.CHECK);

		IAdaptable adapter = getElement();
		Security security = (Security)adapter.getAdapter(Security.class);
		name.setText(security.getName());
		wkn.setText(StringUtils.defaultString(security.getWKN()));
		isin.setText(StringUtils.defaultString(security.getISIN()));
		ticker.setText(StringUtils.defaultString(security.getTicker()));
		morningstarSymbol.setText(StringUtils.defaultString(security.getMorningstarSymbol()));
		fourTradersUrl.setText(StringUtils.defaultString(security.getConfigurationValue("fourTrasdersUrl")));
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
		Security security = (Security)adapter.getAdapter(Security.class);
		security.setName(name.getText());
		security.setWKN(wkn.getText());
		security.setISIN(isin.getText());
		security.setTicker(ticker.getText());
		security.setMorningstarSymbol(morningstarSymbol.getText());
		security.putConfigurationValue("fourTrasdersUrl", fourTradersUrl.getText());
		return super.performOk();
	}

}
