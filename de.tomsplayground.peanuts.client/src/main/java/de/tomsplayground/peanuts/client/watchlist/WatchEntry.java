package de.tomsplayground.peanuts.client.watchlist;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.List;

import org.joda.time.DateTime;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.InventoryEntry;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;
import de.tomsplayground.peanuts.domain.currenncy.ExchangeRates;
import de.tomsplayground.peanuts.domain.fundamental.AvgFundamentalData;
import de.tomsplayground.peanuts.domain.fundamental.CurrencyAjustedFundamentalData;
import de.tomsplayground.peanuts.domain.fundamental.FundamentalData;
import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.statistics.Signal;
import de.tomsplayground.peanuts.domain.statistics.SimpleMovingAverage;
import de.tomsplayground.util.Day;

public class WatchEntry {
	private static final MathContext MC = new MathContext(10, RoundingMode.HALF_EVEN);

	private final Security security;
	private final IPriceProvider adjustedPriceProvider;

	// Cache
	Signal signal = null;
	BigDecimal avgPe = null;
	BigDecimal peDelta = null;
	BigDecimal peRatio = null;

	private BigDecimal currencyAdjustedAvgEpsChange;

	WatchEntry(Security security, IPriceProvider adjustedPriceProvider) {
		this.security = security;
		this.adjustedPriceProvider = adjustedPriceProvider;
	}
	public IPriceProvider getPriceProvider() {
		return adjustedPriceProvider;
	}
	public IPrice getPrice() {
		if (adjustedPriceProvider.getMaxDate() != null) {
			return adjustedPriceProvider.getPrice(adjustedPriceProvider.getMaxDate());
		}
		return Price.ZERO;
	}
	public Security getSecurity() {
		return security;
	}
	public Signal getSignal() {
		if (signal != null) {
			return signal;
		}

		SimpleMovingAverage simpleMovingAverage = new SimpleMovingAverage(20);
		simpleMovingAverage.calculate(adjustedPriceProvider.getPrices());
		List<Signal> signals = simpleMovingAverage.getSignals();
		if (! signals.isEmpty()) {
			signal = signals.get(signals.size() -1);
		} else {
			signal = Signal.NO_SIGNAL;
		}
		return signal;
	}

	public BigDecimal getDayChangeAbsolut() {
		Day now = new Day();
		IPrice price1 = adjustedPriceProvider.getPrice(now.addDays(-1).adjustWorkday());
		IPrice price2 = adjustedPriceProvider.getPrice(now);

		return price2.getClose().subtract(price1.getClose());
	}

	public BigDecimal getDayChange() {
		Day now = new Day();
		IPrice price1 = adjustedPriceProvider.getPrice(now.addDays(-1).adjustWorkday());
		IPrice price2 = adjustedPriceProvider.getPrice(now);

		if (price1.getClose().signum() == 0) {
			return BigDecimal.ZERO;
		}

		BigDecimal delta = price2.getClose().subtract(price1.getClose());

		BigDecimal performance = delta.divide(price1.getClose(), MC);
		return performance;
	}

	public BigDecimal getPerformance(int deltyDays, int deltaMonths, int deltaYear) {
		Day maxDay = adjustedPriceProvider.getMaxDate();
		if (maxDay == null) {
			return BigDecimal.ZERO;
		}
		Day minDay = maxDay.addDays(-deltyDays);
		minDay = minDay.addMonth(-deltaMonths);
		minDay = minDay.addYear(-deltaYear);
		return getPerformance(minDay, maxDay);
	}

	public BigDecimal getCustomPerformance() {
		WatchlistManager manager = WatchlistManager.getInstance();
		if (manager.isCustomPerformanceRangeSet()) {
			Day from = manager.getPerformanceFrom();
			Day to = manager.getPerformanceTo();
			return getPerformance(from, to);
		}
		return BigDecimal.ZERO;
	}

	public BigDecimal getPerformance(Day minDay, Day maxDay) {
		IPrice price1 = adjustedPriceProvider.getPrice(minDay);
		IPrice price2 = adjustedPriceProvider.getPrice(maxDay);
		BigDecimal delta = price2.getClose().subtract(price1.getClose());
		if (price1.getClose().signum() == 0) {
			return BigDecimal.ZERO;
		}
		BigDecimal performance = delta.divide(price1.getClose(), MC);
		return performance;
	}

	private FundamentalData getCurrentFundamentalData() {
		return adjust(security.getCurrentFundamentalData());
	}

	private FundamentalData adjust(FundamentalData fundamentalData) {
		if (fundamentalData == null) {
			return null;
		}
		Currency currency = fundamentalData.getCurrency();
		ExchangeRates exchangeRate = Activator.getDefault().getExchangeRate();
		CurrencyConverter currencyConverter = exchangeRate.createCurrencyConverter(currency, security.getCurrency());
		if (currencyConverter == null) {
			return fundamentalData;
		}
		return new CurrencyAjustedFundamentalData(fundamentalData, currencyConverter);
	}

	private AvgFundamentalData getAvgFundamentalData() {
		List<FundamentalData> fundamentalDatas = security.getFundamentalDatas();
		if (fundamentalDatas.isEmpty()) {
			return new AvgFundamentalData(fundamentalDatas, adjustedPriceProvider, null);
		}
		Currency currency = fundamentalDatas.get(0).getCurrency();
		ExchangeRates exchangeRate = Activator.getDefault().getExchangeRate();
		CurrencyConverter currencyConverter = exchangeRate.createCurrencyConverter(currency, security.getCurrency());
		return new AvgFundamentalData(fundamentalDatas, adjustedPriceProvider, currencyConverter);
	}

	public BigDecimal getPeRatio() {
		if (peRatio != null) {
			return peRatio;
		}
		final FundamentalData data1 = getCurrentFundamentalData();
		if (data1 != null) {
			peRatio = data1.calculatePeRatio(adjustedPriceProvider);
			FundamentalData dataNextYear = Iterables.find(security.getFundamentalDatas(), new Predicate<FundamentalData>() {
				@Override
				public boolean apply(FundamentalData input) {
					return input.getYear() == data1.getYear() + 1;
				}
			}, null);
			if (dataNextYear != null) {
				dataNextYear = adjust(dataNextYear);
				final Day now = new Day();
				int daysThisYear = now.delta(data1.getFiscalEndDay());
				if (daysThisYear < 360) {
					float thisYear = (peRatio.floatValue() * daysThisYear);
					BigDecimal peRatio2 = dataNextYear.calculatePeRatio(adjustedPriceProvider);
					float nextYear = (peRatio2.floatValue() * (360 - daysThisYear));
					peRatio = new BigDecimal((thisYear + nextYear) / 360);
				}
			}
		}
		return peRatio;
	}

	public BigDecimal getDivYield() {
		FundamentalData data1 = getCurrentFundamentalData();
		if (data1 != null) {
			return data1.calculateDivYield(adjustedPriceProvider);
		}
		return null;
	}

	public BigDecimal getYOC(InventoryEntry inventoryEntry) {
		if (inventoryEntry == null) {
			return null;
		}
		FundamentalData data1 = getCurrentFundamentalData();
		if (data1 != null && inventoryEntry.getQuantity().signum() > 0) {
			return data1.calculateYOC(inventoryEntry);
		}
		return null;
	}

	public BigDecimal getDebtEquityRatio() {
		FundamentalData data1 = getCurrentFundamentalData();
		if (data1 != null) {
			return data1.getDebtEquityRatio();
		}
		return null;
	}

	public BigDecimal getAvgPE() {
		if (avgPe != null) {
			return avgPe;
		}
		AvgFundamentalData avgFundamentalData = getAvgFundamentalData();
		if (avgFundamentalData != null) {
			avgPe = avgFundamentalData.getAvgPE();
		}
		return avgPe;
	}

	public BigDecimal getCurrencyAdjustedAvgEpsChange() {
		if (currencyAdjustedAvgEpsChange != null) {
			return currencyAdjustedAvgEpsChange;
		}
		AvgFundamentalData avgFundamentalData = getAvgFundamentalData();
		if (avgFundamentalData != null) {
			currencyAdjustedAvgEpsChange = avgFundamentalData.getCurrencyAdjustedAvgEpsChange().subtract(BigDecimal.ONE);
		}
		return currencyAdjustedAvgEpsChange;
	}

	public BigDecimal getPeDelta() {
		if (peDelta != null) {
			return peDelta;
		}
		BigDecimal v1 = getPeRatio();
		BigDecimal v2 = getAvgPE();
		if (v1 != null && v2 != null && v2.signum() > 0) {
			peDelta = v1.divide(v2, MC).subtract(BigDecimal.ONE);
		} else {
			peDelta = null;
		}
		return peDelta;
	}

	public DateTime getFundamentalDataDate() {
		List<FundamentalData> fundamentalDatas = security.getFundamentalDatas();
		DateTime d = null;
		for (FundamentalData fundamentalData : fundamentalDatas) {
			if (fundamentalData.getLastModifyDate() != null &&
				(d == null || fundamentalData.getLastModifyDate().isAfter(d))) {
				d = fundamentalData.getLastModifyDate();
			}
		}
		return d;
	}

	public void refreshCache() {
		signal = null;
		avgPe = null;
		peDelta = null;
		peRatio = null;
		currencyAdjustedAvgEpsChange = null;
	}
}