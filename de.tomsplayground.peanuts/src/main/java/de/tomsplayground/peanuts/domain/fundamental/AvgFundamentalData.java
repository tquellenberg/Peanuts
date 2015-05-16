package de.tomsplayground.peanuts.domain.fundamental;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;
import de.tomsplayground.peanuts.domain.process.CurrencyAdjustedPriceProvider;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.util.Day;

public class AvgFundamentalData {

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
		final int currentYear = new Day().year;
		return Lists.newArrayList(Iterables.filter(datas, new Predicate<FundamentalData>(){
			@Override
			public boolean apply(FundamentalData input) {
				return input.getYear() < currentYear;
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
				return input.calculatePeRatio(pp).signum() > 0;
			}
		}));
		if (adjustedData.isEmpty()) {
			return BigDecimal.ZERO;
		}
		double sum = 0;
		Map<FundamentalData, BigDecimal> peRatio = Maps.newHashMap();
		for (FundamentalData fundamentalData : adjustedData) {
			BigDecimal ratio = fundamentalData.calculatePeRatio(pp);
			peRatio.put(fundamentalData, ratio);
			sum += ratio.doubleValue();
		}
		final double avg = sum / adjustedData.size();
		// calculate deviation
		double maxDeviation = 0.0;
		FundamentalData spike = null;
		for (Map.Entry<FundamentalData, BigDecimal> entry : peRatio.entrySet()) {
			double deviation = entry.getValue().divide(new BigDecimal(avg), new MathContext(10, RoundingMode.HALF_EVEN))
				.subtract(BigDecimal.ONE)
				.abs().doubleValue();
			if (deviation > 0.3 && deviation > maxDeviation) {
				maxDeviation = deviation;
				spike = entry.getKey();
			}
		}
		if (spike != null) {
			// remove spikes
			adjustedData.remove(spike);
			sum = 0;
			for (FundamentalData fundamentalData : adjustedData) {
				sum += fundamentalData.calculatePeRatio(pp).doubleValue();
			}
		}
		return new BigDecimal(sum / adjustedData.size());
	}

	public BigDecimal getAvgEpsChange() {
		return getAvgEpsChange(getHistoricData());
	}

	public BigDecimal getCurrencyAdjustedAvgEpsChange() {
		return getAvgEpsChange(getAdjustedData(getHistoricData()));
	}

	private BigDecimal getAvgEpsChange(List<FundamentalData> historicData) {
		ArrayList<FundamentalData> validDatas = Lists.newArrayList(Iterables.filter(historicData, new Predicate<FundamentalData>() {
			@Override
			public boolean apply(FundamentalData input) {
				return (input.getEarningsPerShare().signum() > 0);
			}
		}));

		if (validDatas.size() < 2) {
			return BigDecimal.ZERO;
		}
		int i = 1;
		FundamentalData d1 = validDatas.get(0);
		double avg = 1;
		while (i < validDatas.size()) {
			FundamentalData d2 = validDatas.get(i);
			BigDecimal change = d2.getEarningsPerShare().divide(d1.getEarningsPerShare(), new MathContext(10, RoundingMode.HALF_EVEN));
			avg = avg * change.doubleValue();
			// Next
			i++;
			d1 = d2;
		}
		return new BigDecimal(Math.pow(avg, 1.0 / (validDatas.size()-1)), new MathContext(10, RoundingMode.HALF_EVEN));
	}

}
