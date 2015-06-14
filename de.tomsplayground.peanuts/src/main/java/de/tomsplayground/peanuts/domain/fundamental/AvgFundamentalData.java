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
		double sum = 0;
		for (FundamentalData fundamentalData : adjustedData) {
			double ratio = fundamentalData.calculatePeRatio(pp).doubleValue();
			ratio = Math.min(ratio, 35.0);
			sum += ratio;
		}
		return new BigDecimal(sum / adjustedData.size());
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
			return now.subtract(prev).divide(prev.abs(), new MathContext(10, RoundingMode.HALF_EVEN));
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
		ArrayList<FundamentalData> validDatas = Lists.newArrayList(Iterables.filter(historicData, new Predicate<FundamentalData>() {
			@Override
			public boolean apply(FundamentalData input) {
				return (input.getEarningsPerShare().signum() > 0) && ! input.isIgnoreInAvgCalculation()
					&& input.getFiscalEndDay().year >= 2006;
			}
		}));
		BigDecimal calculateAvgEpsGrowth2006 = calculateAvgEpsGrowth(validDatas);

		validDatas = Lists.newArrayList(Iterables.filter(historicData, new Predicate<FundamentalData>() {
			@Override
			public boolean apply(FundamentalData input) {
				return (input.getEarningsPerShare().signum() > 0) && ! input.isIgnoreInAvgCalculation()
					&& input.getFiscalEndDay().year >= 2011;
			}
		}));
		BigDecimal calculateAvgEpsGrowth2011 = calculateAvgEpsGrowth(validDatas);
		if (calculateAvgEpsGrowth2006 == null) {
			return calculateAvgEpsGrowth2011;
		}
		if (calculateAvgEpsGrowth2011 == null) {
			return calculateAvgEpsGrowth2006;
		}
		int compareTo = calculateAvgEpsGrowth2006.compareTo(calculateAvgEpsGrowth2011);
		if (compareTo < 0) {
			return calculateAvgEpsGrowth2006;
		} else {
			return calculateAvgEpsGrowth2011;
		}
	}

	private BigDecimal calculateAvgEpsGrowth(ArrayList<FundamentalData> validDatas) {
		if (validDatas.size() < 2) {
			return null;
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
