package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import de.tomsplayground.util.Day;

public class Price implements ITimedElement, IPrice {

	public final static Price ZERO = new Price(Day.ZERO, BigDecimal.ZERO);
	
	private final Day date;
	private final BigDecimal open;
	private final BigDecimal close;
	private final BigDecimal high;
	private final BigDecimal low;

	public Price(Day date, BigDecimal open, BigDecimal close, BigDecimal high, BigDecimal low) {
		if (date == null) throw new IllegalArgumentException("date");
		if (open == null && close == null) {
			close = BigDecimal.ZERO;
		}
		this.date = date;
		this.open = open;
		this.close = close;
		if (high == null) {
			if (open != null) {
				if (close != null) {
					this.high = open.max(close);
				} else {
					this.high = open;
				}
			} else {
				this.high = close;
			}
		} else {
			this.high = high;
		}
		if (low == null) {
			if (open != null) {
				if (close != null) {
					this.low = open.min(close);
				} else {
					this.low = open;
				}
			} else {
				this.low = close;
			}
		} else {
			this.low = low;
		}
	}

	public Price(Day date, BigDecimal value) {
		this(date, null, value, null, null);
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("date", date).append("close", close).toString();
	}

	@Override
	public Day getDay() {
		return date;
	}

	@Override
	public BigDecimal getOpen() {
		return open;
	}

	@Override
	public BigDecimal getValue() {
		return close;
	}

	@Override
	public BigDecimal getClose() {
		return close;
	}

	@Override
	public BigDecimal getHigh() {
		return high;
	}

	@Override
	public BigDecimal getLow() {
		return low;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(29, 101).
			append(date).
			append(open).
			append(close).
			append(high).
			append(low).
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
		Price p = (Price) obj;
		return date.equals(p.date) &&
			equalsOrNull(open, p.open) &&
			equalsOrNull(close, p.close) &&
			equalsOrNull(high, p.high) &&
			equalsOrNull(low, p.low);
	}
	
	private boolean equalsOrNull(Object o1, Object o2) {
		if (o1 == o2)
			return true;
		if (o1 == null || o2 == null)
			return false;
		return o1.equals(o2);
	}

}
