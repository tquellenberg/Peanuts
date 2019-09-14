package de.tomsplayground.peanuts.domain.dividend;

import static org.apache.commons.lang3.ObjectUtils.*;

import java.math.BigDecimal;
import java.util.Currency;

import org.apache.commons.lang3.StringUtils;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.currenncy.Currencies;
import de.tomsplayground.util.Day;

public class Dividend implements Comparable<Dividend> {

	private Day payDate;

	private BigDecimal amountPerShare;

	private String currency;

	private BigDecimal quantity;

	private BigDecimal amount;

	private BigDecimal amountInDefaultCurrency;

	private BigDecimal taxInDefaultCurrency;

	transient private Security security;

	public Dividend(Dividend d) {
		this.payDate = d.payDate;
		this.amountPerShare = d.amountPerShare;
		this.currency = d.currency;
		this.quantity = d.quantity;
		this.amount = d.amount;
		this.amountInDefaultCurrency = d.amountInDefaultCurrency;
		this.taxInDefaultCurrency = d.taxInDefaultCurrency;
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

	public BigDecimal getTaxInDefaultCurrency() {
		return taxInDefaultCurrency;
	}
	public void setTaxInDefaultCurrency(BigDecimal taxInDefaultCurrency) {
		this.taxInDefaultCurrency = taxInDefaultCurrency;
	}
	public BigDecimal getNettoAmountInDefaultCurrency() {
		return defaultIfNull(getAmountInDefaultCurrency(), BigDecimal.ZERO)
			.subtract(defaultIfNull(getTaxInDefaultCurrency(), BigDecimal.ZERO));
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

}
