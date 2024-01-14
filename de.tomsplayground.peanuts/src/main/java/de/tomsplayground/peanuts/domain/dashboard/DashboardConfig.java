package de.tomsplayground.peanuts.domain.dashboard;

import java.util.ArrayList;
import java.util.List;

public class DashboardConfig {
	
	private List<DashboardWidgetConfig> widgetConfigs = new ArrayList<>();

	public DashboardConfig() {
		widgetConfigs.add(new DashboardWidgetConfig("Apple", 1, 1));
		widgetConfigs.add(new DashboardWidgetConfig("Microsoft", 1, 1));
		widgetConfigs.add(new DashboardWidgetConfig("Adobe", 1, 1));
		widgetConfigs.add(new DashboardWidgetConfig("Souther Copper", 1, 1));

		widgetConfigs.add(new DashboardWidgetConfig("Bitcoin", 2, 1));
		widgetConfigs.add(new DashboardWidgetConfig("Gold", 2, 1));
	}
	
	public List<DashboardWidgetConfig> getWidgetConfigs() {
		return widgetConfigs;
	}
}
