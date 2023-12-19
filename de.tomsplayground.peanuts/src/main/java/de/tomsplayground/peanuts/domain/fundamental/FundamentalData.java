package de.tomsplayground.peanuts.domain.fundamental;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Currency;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.currenncy.Currencies;
import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

@XStreamAlias("fundamental")
public class FundamentalData implements Comparable<FundamentalData> {

	private int year;
	private int ficalYearEndsMonth; // 0: Dez, -1: Nov ...., -11: Jan
	private BigDecimal dividende;
	private BigDecimal earningsPerShare;
	private BigDecimal debtEquityRatio;
	private String currency;
	private LocalDateTime lastModifyDate;
	private boolean ignoreInAvgCalculation;
	private boolean locked;

	transient private Day fiscalStartDay;
	transient private Day fiscalEndDay;

	public FundamentalData() {
		this.year = 2000;
		this.dividende = BigDecimal.ZERO;
		this.earningsPerShare = BigDecimal.ZERO;
		this.debtEquityRatio = BigDecimal.ZERO;
		this.ficalYearEndsMonth = 0;
		this.lastModifyDate = LocalDateTime.now();
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
	}

	public int getYear() {
		return year;
	}
	public void setYear(int year) {
		this.year = year;
		this.fiscalStartDay = null;
		this.fiscalEndDay = null;
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

	public BigDecimal calculateDivYield(IPriceProvider priceProvider) {
		if (! getCurrency().equals(priceProvider.getCurrency())) {
			throw new IllegalArgumentException("Fundamental data and price provider must use same currency. ("+getCurrency()+", "+priceProvider.getCurrency()+")");
		}
		IPrice price = priceProvider.getPrice(getFiscalEndDay());
		BigDecimal close = price.getValue();
		if (close.signum() == 0) {
			return BigDecimal.ZERO;
		}
		return getDividende().divide(close, PeanutsUtil.MC);
	}

	public Day getFiscalStartDay() {
		if (fiscalStartDay == null) {
			fiscalStartDay = Day.firstDayOfYear(year).addMonth(getFicalYearEndsMonthInt());
		}
		return fiscalStartDay;
	}

	public Day getFiscalEndDay() {
		if (fiscalEndDay == null) {
			fiscalEndDay = getFiscalStartDay().addMonth(12).addDays(-1);
		}
		return fiscalEndDay;
	}

	public boolean isIncluded(Day day) {
		return (day.after(getFiscalStartDay()) && day.before(getFiscalEndDay())) ||
			day.equals(getFiscalStartDay()) ||
			day.equals(getFiscalEndDay());
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

	private int getFicalYearEndsMonthInt() {
		return ficalYearEndsMonth;
	}
	
	public Month getFicalYearEndsMonth() {
		// ficalYearEndsMonth: (0 .. -11) => (Dec ... Jan)
		return Month.of(12 + ficalYearEndsMonth);
	}

	public void setFicalYearEndsMonth(Month ficalYearEndsMonth) {
		setFicalYearEndsMonth(-12 + ficalYearEndsMonth.getValue());
	}
	
	private void setFicalYearEndsMonth(int ficalYearEndsMonth) {
		// ficalYearEndsMonth: (0 .. -11) => (Dec ... Jan)
		this.ficalYearEndsMonth = ficalYearEndsMonth;
		this.fiscalStartDay = null;
		this.fiscalEndDay = null;
	}

	public LocalDateTime getLastModifyDate() {
		return lastModifyDate;
	}
	public void updateLastModifyDate() {
		lastModifyDate = LocalDateTime.now();
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

	public void update(FundamentalData newData) {
		updateLastModifyDate();
		if (locked) {
			if (getDebtEquityRatio() == null ||
				(getDebtEquityRatio().signum() == 0 && newData.getDebtEquityRatio().signum() > 0)) {
				setDebtEquityRatio(newData.getDebtEquityRatio());
			}
			return;
		} else {
			if (newData.getDebtEquityRatio().signum() > 0) {
				setDebtEquityRatio(newData.getDebtEquityRatio());
			}
		}
		if (newData.getDividende().signum() > 0) {
			setDividende(newData.getDividende());
		}
		setEarningsPerShare(newData.getEarningsPerShare());
		if (newData.getFicalYearEndsMonthInt() != 0) {
			setFicalYearEndsMonth(newData.getFicalYearEndsMonthInt());
		}
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
	}
}
