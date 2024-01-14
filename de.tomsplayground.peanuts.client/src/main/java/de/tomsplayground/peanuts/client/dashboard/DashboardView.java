package de.tomsplayground.peanuts.client.dashboard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import de.tomsplayground.peanuts.domain.dashboard.DashboardConfig;
import de.tomsplayground.peanuts.domain.dashboard.DashboardWidgetConfig;

public class DashboardView extends ViewPart {
	
	public static final String ID = "de.tomsplayground.peanuts.client.dashboardView";

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
	}
	
	@Override
	public void dispose() {
		super.dispose();
	}
	
	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
	}
	
	@Override
	public void createPartControl(Composite parent) {

		Composite top = new Composite(parent, SWT.BORDER);
		GridLayout layout = new GridLayout(4, true);
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		layout.horizontalSpacing = 10;
		layout.verticalSpacing = 10;
		top.setLayout(layout);
		top.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
		
		DashboardConfig dashboardConfig = new DashboardConfig();
		for (DashboardWidgetConfig widgetConfig : dashboardConfig.getWidgetConfigs()) {
			new AbstractDashboardComposite(top, widgetConfig);
		}
	}

	@Override
	public void setFocus() {
	}

}
