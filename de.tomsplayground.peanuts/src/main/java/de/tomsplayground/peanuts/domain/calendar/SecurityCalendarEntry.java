package de.tomsplayground.peanuts.domain.calendar;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.util.Day;

@XStreamAlias("securityCalendarEntry")
public class SecurityCalendarEntry extends CalendarEntry {

	Security security;

	public SecurityCalendarEntry(Security security, Day day, String name) {
		super(day, name);
		this.security = security;
	}

	public Security getSecurity() {
		return security;
	}
}
