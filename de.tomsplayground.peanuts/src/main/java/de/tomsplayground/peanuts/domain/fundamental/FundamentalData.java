package de.tomsplayground.peanuts.domain.fundamental;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Currency;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import com.google.common.collect.ImmutableList;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.base.InventoryEntry;
import de.tomsplayground.peanuts.domain.currenncy.Currencies;
import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.util.Day;

@XStreamAlias("fundamental")
public class FundamentalData implements Comparable<FundamentalData> {

	private int year;
	private int ficalYearEndsMonth;
	private BigDecimal dividende;
	private BigDecimal earningsPerShare;
	private BigDecimal debtEquityRatio;
	private String currency;
	private DateTime lastModifyDate;
	private boolean ignoreInAvgCalculation;
	private boolean locked;
	private String note;

	public FundamentalData() {
		this.year = 2000;
		this.dividende = BigDecimal.ZERO;
		this.earningsPerShare = BigDecimal.ZERO;
		this.debtEquityRatio = BigDecimal.ZERO;
		this.ficalYearEndsMonth = 0;
		this.lastModifyDate = new DateTime();
	}

	public FundamentalData(FundamentalData d) {
		this.year = d.year;
		this.dividende = d.dividende;
		this.earningsPerShare = d.earningsPerShare;
		this.debtEquityRatio = d.debtEquityRatio;
		this.currency = d.currency;
		this.ficalYearEndsMonth = d.ficalYearEndsMonth;
		this.lastModifyDate = d.lastModifyDate;
		this.ignoreInAvgCalculation = d.ignoreInAvgCalculation;
		this.locked = d.locked;
		this.note = d.note;
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

	protected BigDecimal avgPrice(IPriceProvider priceProvider, int year) {
		Day from = new Day(year,0,1).addMonth(getFicalYearEndsMonth()-6);
		Day to = new Day(year, 11, 31).addMonth(getFicalYearEndsMonth()-6);
		ImmutableList<IPrice> prices = priceProvider.getPrices(from, to);
		if (prices.isEmpty()) {
			return BigDecimal.ZERO;
		}
		BigDecimal sum = BigDecimal.ZERO;
		for (IPrice p : prices) {
			sum = sum.add(p.getClose());
		}
		return sum.divide(new BigDecimal(prices.size()), new MathContext(10, RoundingMode.HALF_EVEN));
	}

	public BigDecimal calculatePeRatio(IPriceProvider priceProvider) {
		BigDecimal price;
		if (getYear() >= new Day().year) {
			price =  priceProvider.getPrice(new Day()).getClose();
		} else {
			price = avgPrice(priceProvider, getYear());
		}
		BigDecimal eps = getEarningsPerShare();
		if (eps.signum() == 0) {
			return BigDecimal.ZERO;
		}
		return price.divide(eps, new MathContext(10, RoundingMode.HALF_EVEN));
	}

	public BigDecimal calculateDivYield(IPriceProvider priceProvider) {
		IPrice price = priceProvider.getPrice(getFiscalEndDay());
		BigDecimal close = price.getClose();
		if (close.signum() == 0) {
			return BigDecimal.ZERO;
		}
		return getDividende().divide(close, new MathContext(10, RoundingMode.HALF_EVEN));
	}

	public Day getFiscalEndDay() {
		return new Day(year, 11, 30).addMonth(getFicalYearEndsMonth());
	}

	public BigDecimal calculateYOC(InventoryEntry inventoryEntry) {
		if (inventoryEntry.getQuantity().signum() <= 0) {
			return BigDecimal.ZERO;
		}
		if (inventoryEntry.getAvgPrice().signum() == 0) {
			return BigDecimal.ZERO;
		}
		return getDividende().divide(inventoryEntry.getAvgPrice(), new MathContext(10, RoundingMode.HALF_EVEN));
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

	@Override
	public int compareTo(FundamentalData o) {
		return Integer.compare(year, o.year);
	}

	public int getFicalYearEndsMonth() {
		return ficalYearEndsMonth;
	}

	public void setFicalYearEndsMonth(int ficalYearEndsMonth) {
		this.ficalYearEndsMonth = ficalYearEndsMonth;
	}

	public DateTime getLastModifyDate() {
		return lastModifyDate;
	}
	public void updateLastModifyDate() {
		lastModifyDate = new DateTime();
	}

	public void setIgnoreInAvgCalculation(boolean ignoreInAvgCalculation) {
		this.ignoreInAvgCalculation = ignoreInAvgCalculation;
	}
	public boolean isIgnoreInAvgCalculation() {
		return ignoreInAvgCalculation;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}
	public boolean isLocked() {
		return locked;
	}
	public String getNote() {
		return StringUtils.defaultString(note);
	}
	public void setNote(String note) {
		this.note = StringUtils.defaultString(note);
	}
}
