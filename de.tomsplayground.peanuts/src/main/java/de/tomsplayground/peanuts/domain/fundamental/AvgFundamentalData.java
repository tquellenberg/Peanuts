package de.tomsplayground.peanuts.domain.fundamental;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;
import de.tomsplayground.peanuts.domain.process.CurrencyAdjustedPriceProvider;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.util.Day;

public class AvgFundamentalData {

	private static final MathContext MC = new MathContext(10, RoundingMode.HALF_EVEN);

	private final List<FundamentalData> datas;
	private final IPriceProvider priceProvider;
	private final CurrencyConverter currencyConverter;

	public AvgFundamentalData(Collection<FundamentalData> datas, IPriceProvider priceProvider, CurrencyConverter currencyConverter) {
		this.priceProvider = priceProvider;
		this.currencyConverter = currencyConverter;
		this.datas = Lists.newArrayList(datas);
		Collections.sort(this.datas);
	}

	private List<FundamentalData> getAdjustedData(List<FundamentalData> datas) {
		if (currencyConverter == null) {
			return datas;
		}
		List<FundamentalData> adjustedData = Lists.newArrayList();
		for (FundamentalData fundamentalData : datas) {
			CurrencyAjustedFundamentalData currencyAjustedData = new CurrencyAjustedFundamentalData(fundamentalData, currencyConverter);
			adjustedData.add(currencyAjustedData);
		}
		return adjustedData;
	}

	private IPriceProvider getPriceProvider() {
		if (currencyConverter == null) {
			return priceProvider;
		}
		return new CurrencyAdjustedPriceProvider(priceProvider, currencyConverter.getInvertedCurrencyConverter());
	}

	private List<FundamentalData> getHistoricData() {
		final Day now = new Day();
		return Lists.newArrayList(Iterables.filter(datas, new Predicate<FundamentalData>(){
			@Override
			public boolean apply(FundamentalData input) {
				return input.getFiscalEndDay().before(now);
			}
		}));
	}

	private List<FundamentalData> getHistoricAndCurrentData() {
		final Day now = new Day();
		return Lists.newArrayList(Iterables.filter(datas, new Predicate<FundamentalData>(){
			@Override
			public boolean apply(FundamentalData input) {
				int delta = now.delta(input.getFiscalEndDay());
				return delta <= 360;
			}
		}));
	}

	public BigDecimal getAvgPE() {
		List<FundamentalData> adjustedData = getHistoricData();
		final IPriceProvider pp = getPriceProvider();
		// remove zero
		adjustedData = Lists.newArrayList(Iterables.filter(adjustedData, new Predicate<FundamentalData>() {
			@Override
			public boolean apply(FundamentalData input) {
				return input.calculatePeRatio(pp).signum() > 0 && ! input.isIgnoreInAvgCalculation();
			}
		}));
		if (adjustedData.isEmpty()) {
			return BigDecimal.ZERO;
		}

		Collections.sort(adjustedData, new Comparator<FundamentalData>() {
			@Override
			public int compare(FundamentalData o1, FundamentalData o2) {
				return o1.calculatePeRatio(pp).compareTo(o2.calculatePeRatio(pp));
			}
		});
		if (adjustedData.size() % 2 == 1) {
			return adjustedData.get(adjustedData.size() / 2).calculatePeRatio(pp);
		} else {
			BigDecimal pe1 = adjustedData.get(adjustedData.size() / 2).calculatePeRatio(pp);
			BigDecimal pe2 = adjustedData.get(adjustedData.size() / 2 - 1).calculatePeRatio(pp);
			return pe1.add(pe2).divide(new BigDecimal("2"), MC);
		}
	}

	public BigDecimal getRobustness() {
		List<FundamentalData> adjustedData = getAdjustedData(getHistoricAndCurrentData());
		adjustedData = Lists.newArrayList(Iterables.filter(adjustedData, new Predicate<FundamentalData>() {
			@Override
			public boolean apply(FundamentalData input) {
				return (input.getEarningsPerShare().signum() > 0) && input.getFiscalEndDay().year >= 2006
					&& ! input.isIgnoreInAvgCalculation();
			}
		}));
		if (adjustedData.size() <= 1) {
			return null;
		}
		BigDecimal robustness = BigDecimal.ONE;
		BigDecimal eps1 = adjustedData.get(0).getEarningsPerShare();
		for (int i = 1; i < adjustedData.size(); i++) {
			BigDecimal eps2 = adjustedData.get(i).getEarningsPerShare();
			BigDecimal growth = growth(eps2, eps1);
			if (growth.signum() == -1) {
				robustness = robustness.add(growth);
			}
			eps1 = eps2;
		}
		if (robustness.signum() == -1) {
			robustness = BigDecimal.ZERO;
		}
		return robustness;
	}

	private BigDecimal growth(BigDecimal now, BigDecimal prev) {
		if (prev.signum() == -1 && now.signum() == 1) {
			return BigDecimal.ZERO;
		}
		if (prev.signum() != 0) {
			return now.subtract(prev).divide(prev.abs(), MC);
		}
		return BigDecimal.ZERO;
	}

	public BigDecimal getAvgEpsGrowth() {
		return getAvgEpsGrowth(getHistoricAndCurrentData());
	}

	public BigDecimal getCurrencyAdjustedAvgEpsGrowth() {
		return getAvgEpsGrowth(getAdjustedData(getHistoricAndCurrentData()));
	}

	private BigDecimal getAvgEpsGrowth(List<FundamentalData> historicData) {
		BigDecimal calculateAvgEpsGrowth5Years = calculateAvgEpsGrowth(historicData, 5);
		BigDecimal calculateAvgEpsGrowth10Years = calculateAvgEpsGrowth(historicData, 10);

		if (calculateAvgEpsGrowth5Years == null) {
			return calculateAvgEpsGrowth10Years;
		}
		if (calculateAvgEpsGrowth10Years == null) {
			return calculateAvgEpsGrowth5Years;
		}
		int compareTo = calculateAvgEpsGrowth5Years.compareTo(calculateAvgEpsGrowth10Years);
		if (compareTo < 0) {
			return calculateAvgEpsGrowth5Years;
		} else {
			return calculateAvgEpsGrowth10Years;
		}
	}

	private BigDecimal calculateAvgEpsGrowth(List<FundamentalData> datas, int years) {
		List<FundamentalData> valideDatas = datas.stream()
			.filter(d -> !d.isIgnoreInAvgCalculation())
			.collect(Collectors.toList());
		if (valideDatas.size() < 3) {
			return null;
		}
		int start = valideDatas.size() - 1 - years;
		if (start < 1) {
			start = 1;
		}
		// Avg value for 3 years
		BigDecimal earningsPerShareStart = valideDatas.get(start-1).getEarningsPerShare();
		earningsPerShareStart = earningsPerShareStart.add(valideDatas.get(start).getEarningsPerShare());
		earningsPerShareStart = earningsPerShareStart.add(valideDatas.get(start+1).getEarningsPerShare());
		earningsPerShareStart = earningsPerShareStart.divide(new BigDecimal(3), MC);

		BigDecimal earningsPerShareEnd = valideDatas.get(valideDatas.size()-1).getEarningsPerShare();

		int yearDelta = valideDatas.get(valideDatas.size()-1).getYear() - valideDatas.get(start).getYear();
		if (earningsPerShareStart.signum() <= 0 || earningsPerShareEnd.signum() <= 0 || yearDelta <= 0) {
			return null;
		}

		BigDecimal change = earningsPerShareEnd.divide(earningsPerShareStart, MC);
		return new BigDecimal(Math.pow(change.doubleValue(), 1.0 / yearDelta), MC);
	}

}
