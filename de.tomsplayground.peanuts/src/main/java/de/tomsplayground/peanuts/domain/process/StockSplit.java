package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.util.PeanutsUtil;
import de.tomsplayground.util.Day;

@XStreamAlias("stocksplit")
public class StockSplit implements ITimedElement {

	private final Security security;
	private final Day day;
	private final int from;
	private final int to;

	public StockSplit(Security security, Day day, int from, int to) {
		if (security == null) {
			throw new IllegalArgumentException("security");
		}
		if (day == null) {
			throw new IllegalArgumentException("day");
		}
		this.security = security;
		this.day = day;
		this.from = from;
		this.to = to;
	}

	public BigDecimal getRatio() {
		return new BigDecimal(from).divide(new BigDecimal(to), PeanutsUtil.MC);
	}

	public Security getSecurity() {
		return security;
	}

	@Override
	public Day getDay() {
		return day;
	}

	public int getFrom() {
		return from;
	}

	public int getTo() {
		return to;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("security", security).append("day", day).
			append("from", from).append("to", to).toString();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(71, 153).
			append(day).
			append(from).
			append(to).
			append(security).
			toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		StockSplit p = (StockSplit) obj;
		return day.equals(p.day) &&
			security.equals(p.security) &&
			from == p.from &&
			to == p.to;
	}

}
