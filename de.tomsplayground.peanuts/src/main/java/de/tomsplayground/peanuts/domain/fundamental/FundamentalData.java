package de.tomsplayground.peanuts.domain.fundamental;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Currency;

import org.apache.commons.lang3.StringUtils;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.base.InventoryEntry;
import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.util.Day;

@XStreamAlias("fundamental")
public class FundamentalData {

	private int year;
	private BigDecimal dividende;
	private BigDecimal earningsPerShare;
	private BigDecimal debtEquityRatio;
	private String currency;

	public FundamentalData() {
		this.year = 2000;
		this.dividende = BigDecimal.ZERO;
		this.earningsPerShare = BigDecimal.ZERO;
		this.debtEquityRatio = BigDecimal.ZERO;
	}

	public FundamentalData(FundamentalData d) {
		this.year = d.year;
		this.dividende = d.dividende;
		this.earningsPerShare = d.earningsPerShare;
		this.debtEquityRatio = d.debtEquityRatio;
		this.currency = d.currency;
	}

	public int getYear() {
		return year;
	}
	public void setYear(int year) {
		this.year = year;
	}
	public BigDecimal getDividende() {
		return dividende;
	}
	public void setDividende(BigDecimal dividende) {
		this.dividende = dividende;
	}
	public BigDecimal getEarningsPerShare() {
		return earningsPerShare;
	}
	public void setEarningsPerShare(BigDecimal earningsPerShare) {
		this.earningsPerShare = earningsPerShare;
	}
	public BigDecimal getDebtEquityRatio() {
		return debtEquityRatio;
	}
	public void setDebtEquityRatio(BigDecimal deptEquityRatio) {
		this.debtEquityRatio = deptEquityRatio;
	}

	public BigDecimal calculatePeRatio(IPriceProvider priceProvider) {
		IPrice price = priceProvider.getPrice(new Day(year, 11, 31));
		BigDecimal close = price.getClose();
		if (earningsPerShare.signum() == 0) {
			return BigDecimal.ZERO;
		}
		return close.divide(earningsPerShare, new MathContext(10, RoundingMode.HALF_EVEN));
	}

	public BigDecimal calculateDivYield(IPriceProvider priceProvider) {
		IPrice price = priceProvider.getPrice(new Day(year, 11, 31));
		BigDecimal close = price.getClose();
		if (close.signum() == 0) {
			return BigDecimal.ZERO;
		}
		return dividende.divide(close, new MathContext(10, RoundingMode.HALF_EVEN));
	}

	public BigDecimal calculateYOC(InventoryEntry inventoryEntry) {
		if (inventoryEntry.getQuantity().signum() <= 0) {
			return BigDecimal.ZERO;
		}
		if (inventoryEntry.getAvgPrice().signum() == 0) {
			return BigDecimal.ZERO;
		}
		return dividende.divide(inventoryEntry.getAvgPrice(), new MathContext(10, RoundingMode.HALF_EVEN));
	}

	public Currency getCurrency() {
		if (StringUtils.isBlank(currency)) {
			return Currency.getInstance("EUR");
		}
		return Currency.getInstance(currency);
	}

	public void setCurrency(Currency currency) {
		this.currency = currency.getCurrencyCode();
	}
}
