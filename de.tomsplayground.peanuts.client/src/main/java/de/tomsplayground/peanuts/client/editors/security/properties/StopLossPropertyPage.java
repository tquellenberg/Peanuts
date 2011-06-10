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

public class StopLossPropertyPage extends PropertyPage {
	
	private Text stop;
	
	public StopLossPropertyPage() {
		noDefaultAndApplyButton();
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent,SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
				
		stop = createTextWithLabel(composite, "Stop");
		
		IAdaptable adapter = getElement();
		Security security = (Security)adapter.getAdapter(Security.class);
		String stopLossValue = StringUtils.defaultString(security.getConfigurationValue("STOPLOSS"));
		stop.setText(stopLossValue);
		
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
		security.putConfigurationValue("STOPLOSS", stop.getText());
		return super.performOk();
	}

}
