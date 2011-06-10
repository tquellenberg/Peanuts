package de.tomsplayground.peanuts.client.editors.security.properties;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PropertyPage;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.Security;

public class ChartPropertyPage extends PropertyPage {

	private Button showAverage;
	private Combo compareWithList;
	private Button showSignals;

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent,SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
				
		Label label = new Label(composite, SWT.NONE);
		label.setText("Show average");
		showAverage = new Button(composite, SWT.CHECK);

		label = new Label(composite, SWT.NONE);
		label.setText("Show signals");
		showSignals = new Button(composite, SWT.CHECK);

		label = new Label(composite, SWT.NONE);
		label.setText("Compare with");
		compareWithList = new Combo(composite, SWT.READ_ONLY);
		ImmutableList<Security> securities = Activator.getDefault().getAccountManager().getSecurities();
		String[] securityNames = Collections2.transform(securities, new Function<Security, String>() {
			@Override
			public String apply(Security input) {
				return input.getName();
			}
		}).toArray(new String[securities.size()]);
		compareWithList.setItems(securityNames);
		compareWithList.add("", 0);

		IAdaptable adapter = getElement();
		Security security = (Security)adapter.getAdapter(Security.class);
		String showAvg = security.getConfigurationValue("SHOW_AVG");
		showAverage.setSelection(Boolean.parseBoolean(showAvg));

		String showSig = security.getConfigurationValue("SHOW_SIGNALS");
		showSignals.setSelection(Boolean.parseBoolean(showSig));

		final String compareWithIsin = security.getConfigurationValue("COMPARE_WITH");
		if (StringUtils.isNotBlank(compareWithIsin)) {
			int index = Iterables.indexOf(securities, new Predicate<Security>() {
				@Override
				public boolean apply(Security input) {
					return input.getISIN().equals(compareWithIsin);
				}
			});
			compareWithList.select(index+1);
		}

		return composite;
	}
	
	@Override
	public boolean performOk() {
		IAdaptable adapter = getElement();
		Security security = (Security)adapter.getAdapter(Security.class);
		security.putConfigurationValue("SHOW_AVG", Boolean.toString(showAverage.getSelection()));
		security.putConfigurationValue("SHOW_SIGNALS", Boolean.toString(showSignals.getSelection()));

		int index = compareWithList.getSelectionIndex();
		String isin = "";
		if (index > 0) {
			final String securityName = compareWithList.getItem(index);
			Security sec = Iterables.find(Activator.getDefault().getAccountManager().getSecurities(),
			new Predicate<Security>() {
				@Override
				public boolean apply(Security input) {
					return input.getName().equals(securityName);
				}
			});
			isin = sec.getISIN();
		}
		security.putConfigurationValue("COMPARE_WITH", isin);
		
		return super.performOk();
	}

}
