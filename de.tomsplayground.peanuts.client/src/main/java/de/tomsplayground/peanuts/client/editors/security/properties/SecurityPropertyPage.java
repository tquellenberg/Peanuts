package de.tomsplayground.peanuts.client.editors.security.properties;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

import de.tomsplayground.peanuts.domain.base.Security;

public class SecurityPropertyPage extends PropertyPage {
	
	private Text name;
	private Text wkn;
	private Text isin;
	private Text ticker;
	
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
		
		IAdaptable adapter = getElement();
		Security security = (Security)adapter.getAdapter(Security.class);
		name.setText(security.getName());
		wkn.setText(StringUtils.defaultString(security.getWKN()));
		isin.setText(StringUtils.defaultString(security.getISIN()));
		ticker.setText(StringUtils.defaultString(security.getTicker()));
		
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
		return super.performOk();
	}

}
