package de.tomsplayground.peanuts.domain.fundamental;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.currenncy.Currencies;
import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;
import de.tomsplayground.peanuts.domain.currenncy.ExchangeRates;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.util.PeanutsUtil;
import de.tomsplayground.util.Day;

public class FundamentalDatas {

	public static final String OVERRIDDEN_AVG_PE = "OverriddenAvgPE";

	private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("360");

	private final ImmutableList<FundamentalData> fundamentalDatas;
	private final Security security;

	public FundamentalDatas(List<FundamentalData> datas, Security security) {
		this.security = security;
		this.fundamentalDatas = ImmutableList.copyOf(datas);
	}

	public FundamentalData getCurrentFundamentalData() {
		return getFundamentalData(new Day());
	}

	public FundamentalData getFundamentalData(Day day) {
		return fundamentalDatas.stream()
			.filter(d -> d.isIncluded(day))
			.findAny().orElse(null);
	}

	public AvgFundamentalData getAvgFundamentalData(IPriceProvider adjustedPriceProvider, ExchangeRates exchangeRates) {
		CurrencyConverter currencyConverter = exchangeRates.createCurrencyConverter(getCurrency(), security.getCurrency());
		return new AvgFundamentalData(fundamentalDatas, adjustedPriceProvider, currencyConverter);
	}

	public BigDecimal getOverriddenAvgPE() {
		String overriddenAvgPE = security.getConfigurationValue(OVERRIDDEN_AVG_PE);
		if (StringUtils.isBlank(overriddenAvgPE)) {
			return null;
		}
		return new BigDecimal(overriddenAvgPE);
	}

	public Optional<DateTime> getMaxModificationDate() {
		return fundamentalDatas.stream()
			.map(d -> d.getLastModifyDate())
			.filter(a -> a != null)
			.reduce((a, b) -> (a.compareTo(b) > 0) ? a : b);
	}

	public boolean isEmpty() {
		return fundamentalDatas.isEmpty();
	}

	public Currency getCurrency() {
		if (fundamentalDatas.isEmpty()) {
			return Currencies.getInstance().getDefaultCurrency();
		}
		return fundamentalDatas.get(0).getCurrency();
	}

	public Security getSecurity() {
		return security;
	}

	public ImmutableList<FundamentalData> getDatas() {
		return fundamentalDatas;
	}

	/**
	 * Adjusted to the security currency.
	 */
	public BigDecimal getAdjustedContinuousEarnings(Day day, Currency toCurrency, ExchangeRates exchangeRates) {
		BigDecimal earnings = getContinuousEarnings(earningOffset(day));
		if (earnings == null) {
			return null;
		}
		CurrencyConverter currencyConverter = exchangeRates.createCurrencyConverter(getCurrency(), toCurrency);
		return currencyConverter.convert(earnings, day);
	}

	private Day earningOffset(Day day) {
		return day.addMonth(+6);
	}

	/**
	 * Adjusted earnings for a given day (in currency of {@link #getCurrency()}).
	 */
	BigDecimal getContinuousEarnings(Day day) {
		day = day.addYear(-1);
		FundamentalData fundamentalData1 = getFundamentalData(day);
		FundamentalData fundamentalData2 = getFundamentalData(day.addYear(1));
		if (fundamentalData1 == null && fundamentalData2 == null) {
			return null;
		}
		if (fundamentalData1 == null) {
			return fundamentalData2.getEarningsPerShare();
		}
		if (fundamentalData2 == null) {
			return fundamentalData1.getEarningsPerShare();
		}
		int daysYear1 = day.delta(fundamentalData1.getFiscalEndDay());
		BigDecimal earnings1  = fundamentalData1.getEarningsPerShare().multiply(new BigDecimal(daysYear1));
		BigDecimal earnings2  = fundamentalData2.getEarningsPerShare().multiply(new BigDecimal(360 - daysYear1));

		return earnings1.add(earnings2).divide(DAYS_IN_YEAR, PeanutsUtil.MC);
	}
}
