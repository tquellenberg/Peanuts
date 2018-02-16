package de.tomsplayground.peanuts.domain.alarm;

import java.math.BigDecimal;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.util.Day;

@XStreamAlias("securityAlarm")
public class SecurityAlarm {

	public enum Type {
		CROSSES_FROM_BELOW,
		CROSSES_FROM_ABOVE
	}

	private final Security security;
	private final Type type;
	private final BigDecimal value;
	private final Day startDay;

	private Day triggerDay;

	public SecurityAlarm(Security security, Type type, BigDecimal value) {
		this.security = security;
		this.type = type;
		this.value = value;
		this.startDay = new Day();
		this.triggerDay = null;
	}

	public Security getSecurity() {
		return security;
	}
	public Type getType() {
		return type;
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
