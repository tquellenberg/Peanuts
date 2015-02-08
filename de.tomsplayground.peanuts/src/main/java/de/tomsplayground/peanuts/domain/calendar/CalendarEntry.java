package de.tomsplayground.peanuts.domain.calendar;

import de.tomsplayground.util.Day;

public class CalendarEntry {

	private final Day day;
	private final String name;

	public CalendarEntry(Day day, String name) {
		this.day = day;
		this.name = name;
	}

	public Day getDay() {
		return day;
	}

	public String getName() {
		return name;
	}
}
