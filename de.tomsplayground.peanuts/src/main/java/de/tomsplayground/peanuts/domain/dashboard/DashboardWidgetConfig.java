package de.tomsplayground.peanuts.domain.dashboard;

public class DashboardWidgetConfig {
	
	private String name;
	
	private int hSpan = 1;
	private int vSpan = 1;

	public DashboardWidgetConfig(String name, int hSpan, int vSpan) {
		this.name = name;
		this.hSpan = hSpan;
		this.vSpan = vSpan;
	}
	
	public int gethSpan() {
		return hSpan;
	}
	
	public int getvSpan() {
		return vSpan;
	}
	
	public String getName() {
		return name;
	}
}
