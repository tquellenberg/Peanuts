package de.tomsplayground.peanuts.domain.fundamental;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import org.joda.time.DateTime;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.currenncy.Currencies;
import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;
import de.tomsplayground.peanuts.domain.currenncy.ExchangeRates;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.util.Day;

public class FundamentalDatas {

	private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("360");

	private static final MathContext MC = new MathContext(10, RoundingMode.HALF_EVEN);

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
		if (fundamentalDatas.isEmpty()) {
			return null;
		}
		return Iterables.find(fundamentalDatas, new Predicate<FundamentalData>() {
			@Override
			public boolean apply(FundamentalData input) {
				return (day.after(input.getFiscalStartDay()) &&
					   day.before(input.getFiscalEndDay())) ||
					day.equals(input.getFiscalStartDay()) ||
					day.equals(input.getFiscalEndDay());
			}
		}, null);
	}

	public AvgFundamentalData getAvgFundamentalData(IPriceProvider adjustedPriceProvider, ExchangeRates exchangeRates) {
		CurrencyConverter currencyConverter = exchangeRates.createCurrencyConverter(getCurrency(), security.getCurrency());
		return new AvgFundamentalData(fundamentalDatas, adjustedPriceProvider, currencyConverter);
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

	public BigDecimal getAdjustedContinuousEarnings(Day day, ExchangeRates exchangeRates) {
		BigDecimal pe = getContinuousEarnings(earningOffset(day));
		if (pe == null) {
			return null;
		}
		CurrencyConverter currencyConverter = exchangeRates.createCurrencyConverter(getCurrency(), security.getCurrency());
		return currencyConverter.convert(pe, day);
	}

	private Day earningOffset(Day day) {
		return day.addMonth(+6);
	}

	public BigDecimal getContinuousEarnings(Day day) {
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
		BigDecimal pe1  = fundamentalData1.getEarningsPerShare().multiply(new BigDecimal(daysYear1));
		BigDecimal pe2  = fundamentalData2.getEarningsPerShare().multiply(new BigDecimal(360 - daysYear1));

		return pe1.add(pe2).divide(DAYS_IN_YEAR, MC);
	}
}
