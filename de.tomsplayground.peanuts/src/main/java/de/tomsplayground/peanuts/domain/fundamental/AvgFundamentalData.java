package de.tomsplayground.peanuts.domain.fundamental;

import static org.apache.commons.lang3.Validate.*;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.util.PeanutsUtil;
import de.tomsplayground.util.Day;

public class AvgFundamentalData {

	private final List<FundamentalData> datas;
	private final IPriceProvider priceProvider;
	private final CurrencyConverter currencyConverter;

	/**
	 * Created by {@link FundamentalDatas#getAvgFundamentalData(IPriceProvider, de.tomsplayground.peanuts.domain.currenncy.ExchangeRates)}.
	 */
	AvgFundamentalData(Collection<FundamentalData> datas, IPriceProvider priceProvider, CurrencyConverter currencyConverter) {
		notNull(priceProvider);
		notNull(currencyConverter);
		this.priceProvider = priceProvider;
		this.currencyConverter = currencyConverter;
		this.datas = Lists.newArrayList(datas);
		if (!this.datas.isEmpty() && !this.datas.get(0).getCurrency().equals(currencyConverter.getFromCurrency())) {
			throw new IllegalArgumentException("Fundamental data and currency converter (from) must use same currency. ("
				+this.datas.get(0).getCurrency()+", "+currencyConverter.getFromCurrency()+")");
		}
		if (! this.priceProvider.getCurrency().equals(currencyConverter.getToCurrency())) {
			throw new IllegalArgumentException("Price provider and currency converter (to) must use same currency. ("
				+this.priceProvider.getCurrency()+", "+currencyConverter.getToCurrency()+")");
		}
		Collections.sort(this.datas);
	}

	private List<CurrencyAjustedFundamentalData> getAdjustedData(List<FundamentalData> datas) {
		List<CurrencyAjustedFundamentalData> adjustedData = Lists.newArrayList();
		for (FundamentalData fundamentalData : datas) {
			adjustedData.add(new CurrencyAjustedFundamentalData(fundamentalData, currencyConverter));
		}
		return adjustedData;
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
		List<CurrencyAjustedFundamentalData> adjustedData = getAdjustedData(getHistoricData()).stream()
			.filter(input -> input.calculatePeRatio(priceProvider).signum() > 0 && ! input.isIgnoreInAvgCalculation())
			.collect(Collectors.toList());

		if (adjustedData.isEmpty()) {
			return BigDecimal.ZERO;
		}

		Collections.sort(adjustedData, (o1, o2) -> o1.calculatePeRatio(priceProvider).compareTo(o2.calculatePeRatio(priceProvider)));

		if (adjustedData.size() % 2 == 1) {
			return adjustedData.get(adjustedData.size() / 2).calculatePeRatio(priceProvider);
		} else {
			BigDecimal pe1 = adjustedData.get(adjustedData.size() / 2).calculatePeRatio(priceProvider);
			BigDecimal pe2 = adjustedData.get(adjustedData.size() / 2 - 1).calculatePeRatio(priceProvider);
			return pe1.add(pe2).divide(new BigDecimal("2"), PeanutsUtil.MC);
		}
	}

	public BigDecimal getRobustness() {
		List<CurrencyAjustedFundamentalData> adjustedData = getAdjustedData(getHistoricAndCurrentData());
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
			return now.subtract(prev).divide(prev.abs(), PeanutsUtil.MC);
		}
		return BigDecimal.ZERO;
	}

	public BigDecimal getAvgEpsGrowth() {
		return getAvgEpsGrowth(getHistoricAndCurrentData());
	}

	public BigDecimal getCurrencyAdjustedAvgEpsGrowth() {
		return getAvgEpsGrowth(getAdjustedData(getHistoricAndCurrentData()));
	}

	private BigDecimal getAvgEpsGrowth(List<? extends FundamentalData> historicData) {
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

	private BigDecimal calculateAvgEpsGrowth(List<? extends FundamentalData> datas, int years) {
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
		earningsPerShareStart = earningsPerShareStart.divide(new BigDecimal(3), PeanutsUtil.MC);

		BigDecimal earningsPerShareEnd = valideDatas.get(valideDatas.size()-1).getEarningsPerShare();

		int yearDelta = valideDatas.get(valideDatas.size()-1).getYear() - valideDatas.get(start).getYear();
		if (earningsPerShareStart.signum() <= 0 || earningsPerShareEnd.signum() <= 0 || yearDelta <= 0) {
			return null;
		}

		BigDecimal change = earningsPerShareEnd.divide(earningsPerShareStart, PeanutsUtil.MC);
		return new BigDecimal(Math.pow(change.doubleValue(), 1.0 / yearDelta), PeanutsUtil.MC);
	}

}
