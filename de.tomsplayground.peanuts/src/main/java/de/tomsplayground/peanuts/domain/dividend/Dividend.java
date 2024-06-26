package de.tomsplayground.peanuts.domain.dividend;

import static org.apache.commons.lang3.ObjectUtils.*;

import java.math.BigDecimal;
import java.util.Currency;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.currenncy.Currencies;
import de.tomsplayground.peanuts.util.Day;

@XStreamAlias("dividend")
public class Dividend implements Comparable<Dividend> {
	
	public enum Change {
		NONE,
		INCREASE,
		DECREASE
	}

	private Day payDate;

	private BigDecimal amountPerShare;

	private String currency;

	private BigDecimal quantity;

	private BigDecimal amount;

	private BigDecimal amountInDefaultCurrency;
	
	private BigDecimal withholdingTaxInDefaultCurrency;

	private BigDecimal taxInDefaultCurrency;

	private boolean increase;
	
	private Change change;

	transient private Security security;

	public Dividend(Dividend d) {
		this.payDate = d.payDate;
		this.amountPerShare = d.amountPerShare;
		this.currency = d.currency;
		this.quantity = d.quantity;
		this.amount = d.amount;
		this.amountInDefaultCurrency = d.amountInDefaultCurrency;
		this.taxInDefaultCurrency = d.taxInDefaultCurrency;
		this.withholdingTaxInDefaultCurrency = d.withholdingTaxInDefaultCurrency;
		this.security = d.security;
		this.increase = d.increase;
		this.change = d.change;
	}

	public Dividend(Day payDate, BigDecimal amountPerShare, Currency curreny) {
		this.payDate = payDate;
		this.amountPerShare = amountPerShare;
		this.currency = curreny.getCurrencyCode();
	}

	public Day getPayDate() {
		return payDate;
	}
	public void setPayDate(Day payDate) {
		this.payDate = payDate;
	}
	public BigDecimal getAmountPerShare() {
		return amountPerShare;
	}
	public void setAmountPerShare(BigDecimal amountPerShare) {
		this.amountPerShare = amountPerShare;
	}
	public Currency getCurrency() {
		if (StringUtils.isBlank(currency)) {
			return Currencies.getInstance().getDefaultCurrency();
		}
		return Currency.getInstance(currency);
	}
	public void setCurrency(Currency currency) {
		this.currency = currency.getCurrencyCode();
	}
	public BigDecimal getQuantity() {
		return quantity;
	}
	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}
	public BigDecimal getAmount() {
		return amount;
	}
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
	public BigDecimal getAmountInDefaultCurrency() {
		return amountInDefaultCurrency;
	}
	public void setAmountInDefaultCurrency(BigDecimal amountInDefaultCurrency) {
		this.amountInDefaultCurrency = amountInDefaultCurrency;
	}

	public BigDecimal getWithholdingTaxInDefaultCurrency() {
		return withholdingTaxInDefaultCurrency;
	}
	public void setWithholdingTaxInDefaultCurrency(BigDecimal withholdingTaxInDefaultCurrency) {
		this.withholdingTaxInDefaultCurrency = withholdingTaxInDefaultCurrency;
	}

	public BigDecimal getTaxInDefaultCurrency() {
		return taxInDefaultCurrency;
	}
	public void setTaxInDefaultCurrency(BigDecimal taxInDefaultCurrency) {
		this.taxInDefaultCurrency = taxInDefaultCurrency;
	}
	
	public BigDecimal getTaxSumInDefaultCurrency() {
		return defaultIfNull(getWithholdingTaxInDefaultCurrency(), BigDecimal.ZERO)
				.add(defaultIfNull(getTaxInDefaultCurrency(), BigDecimal.ZERO));
	}

	public BigDecimal getNettoAmountInDefaultCurrency() {
		return defaultIfNull(getAmountInDefaultCurrency(), BigDecimal.ZERO)
			.subtract(getTaxSumInDefaultCurrency());
	}
	
	public Security getSecurity() {
		return security;
	}
	public void setSecurity(Security security) {
		this.security = security;
	}

	@Override
	public int compareTo(Dividend o) {
		return payDate.compareTo(o.payDate);
	}

	public Change getChange() {
		if (change == null) {
			return increase?Change.INCREASE:Change.NONE;
		}
		return change;
	}
	
	public void setChange(Change change) {
		this.change = change;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

}
