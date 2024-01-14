package de.tomsplayground.peanuts.client.dashboard;

import java.math.BigDecimal;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.dashboard.DashboardWidgetConfig;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class AbstractDashboardComposite {

	private DashboardWidgetConfig widgetConfig;
	private Composite composite;

	public AbstractDashboardComposite(Composite parent, DashboardWidgetConfig widgetConfig) {
		this.widgetConfig = widgetConfig;
		this.composite = new Composite(parent, SWT.NONE);
		this.composite.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		
		configLayout();
		
		Label l = new Label(composite, SWT.NONE);
		l.setText(widgetConfig.getName());
		
		Composite valueComposite = new Composite(composite, SWT.BORDER);
		GridLayout layout = new GridLayout(2, false);
		valueComposite.setLayout(layout);
		valueComposite.setLayoutData(new GridData (SWT.FILL, SWT.CENTER, true, false));
		
		Label v = new Label(valueComposite, SWT.NONE);
		v.setFont(Activator.getDefault().getBigFont());
		v.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED));
		v.setText(PeanutsUtil.formatPercent(new BigDecimal(-0.03)));
		GridData gridData = new GridData (SWT.FILL, SWT.CENTER, true, false);
		gridData.verticalSpan = 2;
		v.setLayoutData(gridData);
		
		v = new Label(valueComposite, SWT.NONE);
		v.setFont(Activator.getDefault().getSmallFont());
		v.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		v.setText(PeanutsUtil.formatPercent(new BigDecimal(0.05)));

		v = new Label(valueComposite, SWT.NONE);
		v.setFont(Activator.getDefault().getSmallFont());
		v.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		v.setText(PeanutsUtil.formatPercent(new BigDecimal(0.10)));
	}

	private void configLayout() {
		composite.setLayout(new GridLayout());
		
		GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		layoutData.horizontalSpan = widgetConfig.gethSpan();
		layoutData.verticalSpan = widgetConfig.getvSpan();
		composite.setLayoutData(layoutData);
	}

}
