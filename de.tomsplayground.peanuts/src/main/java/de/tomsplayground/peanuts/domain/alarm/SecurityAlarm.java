package de.tomsplayground.peanuts.domain.alarm;

import java.math.BigDecimal;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.util.Day;

@XStreamAlias("securityAlarm")
public class SecurityAlarm {

	public enum Mode {
		PRICE_BELOW,
		PRICE_ABOVE
	}

	private final Security security;
	private final Mode mode;
	private final BigDecimal value;
	private final Day startDay;

	private Day triggerDay;

	public SecurityAlarm(Security security, Mode type, BigDecimal value, Day startDay) {
		this.security = security;
		this.mode = type;
		this.value = value;
		this.startDay = startDay;
		this.triggerDay = null;
	}

	public Security getSecurity() {
		return security;
	}
	public Mode getMode() {
		return mode;
	}
	public BigDecimal getValue() {
		return value;
	}
	public Day getStartDay() {
		return startDay;
	}
	public void setTriggerDay(Day triggerDay) {
		this.triggerDay = triggerDay;
	}
	public Day getTriggerDay() {
		return triggerDay;
	}
	public boolean isTriggered() {
		return triggerDay != null;
	}
	public void trigger(Day day) {
		triggerDay = day;
	}
	public void clearTrigger() {
		triggerDay = null;
	}
}
