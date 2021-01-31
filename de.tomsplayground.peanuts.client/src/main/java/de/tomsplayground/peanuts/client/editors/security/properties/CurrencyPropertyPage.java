package de.tomsplayground.peanuts.client.editors.security.properties;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PropertyPage;

import de.tomsplayground.peanuts.client.widgets.CurrencyComboViewer;
import de.tomsplayground.peanuts.domain.base.Security;

public class CurrencyPropertyPage extends PropertyPage {

	private Security security;
	private CurrencyComboViewer currencyComboViewer;
	private CurrencyComboViewer currencyComboViewer2;

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent,SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		IAdaptable adapter = getElement();
		security = adapter.getAdapter(Security.class);

		Label label = new Label(composite, SWT.NONE);
		label.setText("Currency");
		currencyComboViewer = new CurrencyComboViewer(composite, false, false);
		currencyComboViewer.selectCurrency(security.getCurrency());

		label = new Label(composite, SWT.NONE);
		label.setText("Exchange rate");
		currencyComboViewer2 = new CurrencyComboViewer(composite, true, true);
		if (security.getExchangeCurrency() != null) {
			currencyComboViewer2.selectCurrency(security.getExchangeCurrency());
		}

		return composite;
	}

	@Override
	public boolean performOk() {
		security.setCurreny(currencyComboViewer.getSelectedCurrency());
		security.setExchangeCurreny(currencyComboViewer2.getSelectedCurrency());
		return true;
	}

}
