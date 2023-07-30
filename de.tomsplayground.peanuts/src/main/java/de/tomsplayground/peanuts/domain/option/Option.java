package de.tomsplayground.peanuts.domain.option;

import java.math.BigDecimal;
import java.util.Currency;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import de.tomsplayground.peanuts.util.Day;

public class Option {
	
	public enum Type {
		Call, Put
	}
	
	private Type type;
	private String underlying;
	private BigDecimal strike;
	private Day expiration;

	private int underlyingConId;
	private String underlyingExchange;
	private Currency currency;
	
	public Option(Type type, String underlying, int underlyingConId, String underlyingExchange,
			BigDecimal strike, Currency currency, Day expiration) {
		super();
		this.type = type;
		this.underlying = underlying;
		this.strike = strike;
		this.currency = currency;
		this.expiration = expiration;
		this.underlyingConId = underlyingConId;
		this.underlyingExchange = underlyingExchange;
	}

	public String getDescription() {
		return underlying+" "+expiration+" "+strike+" "+type.toString();
	}
	
	public Type getType() {
		return type;
	}

	public String getUnderlying() {
		return underlying;
	}

	public int getUnderlyingConId() {
		return underlyingConId;
	}
	
	public String getUnderlyingExchange() {
		return underlyingExchange;
	}
	
	public BigDecimal getStrike() {
		return strike;
	}

	public Currency getCurrency() {
		return currency;
	}

	public Day getExpiration() {
		return expiration;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
	
	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj, "underlyingConId", "underlyingExchange");
	}
	
	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this, "underlyingConId", "underlyingExchange");
	}

}
