package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import de.tomsplayground.peanuts.util.Day;

public class Price implements IPrice {

	public final static Price ZERO = new Price(Day.ZERO, BigDecimal.ZERO);

	private final Day date;
	private final BigDecimal close;

	public Price(Day date, BigDecimal close) {
		Validate.notNull(date);
		Validate.notNull(close);

		this.date = date;
		this.close = close;
	}

	@Override
	public Day getDay() {
		return date;
	}

	@Override
	public BigDecimal getValue() {
		return close;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(29, 101).
			append(date).
			append(close).
			toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj, false);
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
	}

}
