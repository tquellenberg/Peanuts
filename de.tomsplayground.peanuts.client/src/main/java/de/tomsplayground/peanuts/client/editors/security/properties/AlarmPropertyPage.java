package de.tomsplayground.peanuts.client.editors.security.properties;

import java.math.BigDecimal;
import java.text.ParseException;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

import com.google.common.collect.ImmutableSet;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.widgets.DateComposite;
import de.tomsplayground.peanuts.domain.alarm.SecurityAlarm;
import de.tomsplayground.peanuts.domain.alarm.SecurityAlarm.Type;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.util.PeanutsUtil;
import de.tomsplayground.util.Day;

public class AlarmPropertyPage extends PropertyPage {

	private Text stop;
	private DateComposite date;

	public AlarmPropertyPage() {
		noDefaultAndApplyButton();
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent,SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		date = createDateWithLabel(composite, "Start date");
		stop = createTextWithLabel(composite, "Value");

		IAdaptable adapter = getElement();
		Security security = adapter.getAdapter(Security.class);

		ImmutableSet<SecurityAlarm> alarm = Activator.getDefault().getAccountManager().getSecurityAlarms(security);
		if (! alarm.isEmpty()) {
			SecurityAlarm stopLoss = alarm.iterator().next();
			date.setDay(stopLoss.getStartDay());
			stop.setText(PeanutsUtil.formatQuantity(stopLoss.getValue()));
		} else {
			date.setDay(new Day());
			stop.setText("");
		}
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

	private DateComposite createDateWithLabel(Composite group, String labelText) {
		Label label = new Label(group, SWT.NONE);
		label.setText(labelText);
		DateComposite date = new DateComposite(group, SWT.NONE);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		date.setLayoutData(gridData);
		return date;
	}

	@Override
	public boolean performOk() {
		Security security = getElement().getAdapter(Security.class);

		AccountManager accountManager = Activator.getDefault().getAccountManager();
		ImmutableSet<SecurityAlarm> stopLosses = accountManager.getSecurityAlarms(security);
		if (! stopLosses.isEmpty()) {
			accountManager.removeSecurityAlarm(stopLosses.iterator().next());
		}

		Day startDate = date.getDay();
		BigDecimal stopValue;
		try {
			stopValue = PeanutsUtil.parseCurrency(stop.getText());
		} catch (ParseException e1) {
			stopValue = BigDecimal.ZERO;
		}
		SecurityAlarm stopLoss = new SecurityAlarm(security, Type.CROSSES_FROM_BELOW, stopValue);
		if (stopValue.compareTo(BigDecimal.ZERO) != 0) {
			accountManager.addSecurityAlarm(stopLoss);
		}
		return super.performOk();
	}

}
