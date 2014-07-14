package de.tomsplayground.peanuts.client.editors.security.properties;

import java.math.BigDecimal;
import java.text.ParseException;

import org.apache.commons.lang3.StringUtils;
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
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.ITrailingStrategy;
import de.tomsplayground.peanuts.domain.process.NoTrailingStrategy;
import de.tomsplayground.peanuts.domain.process.PercentTrailingStrategy;
import de.tomsplayground.peanuts.domain.process.StopLoss;
import de.tomsplayground.peanuts.util.PeanutsUtil;
import de.tomsplayground.util.Day;

public class StopLossPropertyPage extends PropertyPage {
	
	private Text stop;
	private DateComposite date;
	private Text trailingDistancePercent;
	
	public StopLossPropertyPage() {
		noDefaultAndApplyButton();
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent,SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		date = createDateWithLabel(composite, "Start date");
		stop = createTextWithLabel(composite, "Stop");
		trailingDistancePercent = createTextWithLabel(composite, "Trailing distance in percent (0..100%)");
		
		IAdaptable adapter = getElement();
		Security security = (Security)adapter.getAdapter(Security.class);
		
		ImmutableSet<StopLoss> stopLosses = Activator.getDefault().getAccountManager().getStopLosses(security);
		if (! stopLosses.isEmpty()) {
			StopLoss stopLoss = stopLosses.iterator().next();
			date.setDay(stopLoss.getStart());
			stop.setText(PeanutsUtil.formatQuantity(stopLoss.getStartPrice()));
			ITrailingStrategy strategy = stopLoss.getStrategy();
			if (strategy instanceof PercentTrailingStrategy) {
				PercentTrailingStrategy pts = (PercentTrailingStrategy)strategy;
				trailingDistancePercent.setText(PeanutsUtil.formatPercent(pts.getPercent()));
			} else {
				trailingDistancePercent.setText("");
			}
		} else {
			String stopLossValue = StringUtils.defaultString(security.getConfigurationValue("STOPLOSS"));
			date.setDay(new Day(1970, 0, 1));
			stop.setText(stopLossValue);
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
		Security security = (Security)getElement().getAdapter(Security.class);

		AccountManager accountManager = Activator.getDefault().getAccountManager();
		ImmutableSet<StopLoss> stopLosses = accountManager.getStopLosses(security);
		if (! stopLosses.isEmpty()) {
			accountManager.removeStopLoss(stopLosses.iterator().next());
		}
		
		Day startDate = date.getDay();
		BigDecimal stopValue;
		try {
			stopValue = PeanutsUtil.parseCurrency(stop.getText());
		} catch (ParseException e1) {
			stopValue = BigDecimal.ZERO;
		}
		String distance = trailingDistancePercent.getText();
		ITrailingStrategy strategy;
		if (StringUtils.isEmpty(distance)) {
			strategy = new NoTrailingStrategy();
		} else {
			try {
				strategy = new PercentTrailingStrategy(PeanutsUtil.parsePercent(distance));
			} catch (ParseException e) {
				strategy = new NoTrailingStrategy();
			}
		}
		StopLoss stopLoss = new StopLoss(security, startDate, stopValue, strategy);
		if (stopLoss.getStartPrice().compareTo(BigDecimal.ZERO) != 0) {
			accountManager.addStopLoss(stopLoss);
		}
		return super.performOk();
	}

}
