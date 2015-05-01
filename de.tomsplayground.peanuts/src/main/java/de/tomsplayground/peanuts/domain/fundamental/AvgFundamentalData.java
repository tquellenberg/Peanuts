package de.tomsplayground.peanuts.domain.fundamental;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;
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
		List<FundamentalData> adjustedData = getAdjustedData(getHistoricData());
		// remove zero
		adjustedData = Lists.newArrayList(Iterables.filter(adjustedData, new Predicate<FundamentalData>() {
			@Override
			public boolean apply(FundamentalData input) {
				return input.calculatePeRatio(priceProvider).signum() != 0;
			}
		}));
		if (adjustedData.isEmpty()) {
			return BigDecimal.ZERO;
		}
		double sum = 0;
		for (FundamentalData fundamentalData : adjustedData) {
			sum += fundamentalData.calculatePeRatio(priceProvider).doubleValue();
		}
		final double avg = sum / adjustedData.size();
		// remove spikes
		adjustedData = Lists.newArrayList(Iterables.filter(adjustedData, new Predicate<FundamentalData>() {
			@Override
			public boolean apply(FundamentalData input) {
				return Math.abs((input.calculatePeRatio(priceProvider).doubleValue() / avg) - 1) < 0.3;
			}
		}));
		if (adjustedData.isEmpty()) {
			return new BigDecimal(avg);
		}
		sum = 0;
		for (FundamentalData fundamentalData : adjustedData) {
			sum += fundamentalData.calculatePeRatio(priceProvider).doubleValue();
		}
		return new BigDecimal(sum / adjustedData.size());
	}

	public BigDecimal getAvgEpsChange() {
		List<FundamentalData> historicData = getHistoricData();
		ArrayList<FundamentalData> nonEmptyDatas = Lists.newArrayList(Iterables.filter(historicData, new Predicate<FundamentalData>() {
			@Override
			public boolean apply(FundamentalData input) {
				return (input.getEarningsPerShare().signum() != 0);
			}
		}));

		if (nonEmptyDatas.size() < 2) {
			return BigDecimal.ZERO;
		}
		int i = 1;
		FundamentalData d1 = nonEmptyDatas.get(0);
		double avg = 1;
		while (i < nonEmptyDatas.size()) {
			FundamentalData d2 = nonEmptyDatas.get(i);
			BigDecimal change = d2.getEarningsPerShare().divide(d1.getEarningsPerShare(), new MathContext(10, RoundingMode.HALF_EVEN));
			avg = avg * change.doubleValue();
			// Next
			i++;
			d1 = d2;
		}
		return new BigDecimal(Math.pow(avg, 1.0 / (historicData.size()-1)), new MathContext(10, RoundingMode.HALF_EVEN));
	}

}
