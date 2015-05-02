package de.tomsplayground.peanuts.client.watchlist;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.List;

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
		List<IPrice> prices = adjustedPriceProvider.getPrices();
		if (prices.size() < 2) {
			return BigDecimal.ZERO;
		}

		IPrice price1 = prices.get(prices.size() - 2);
		IPrice price2 = prices.get(prices.size() - 1);

		return price2.getClose().subtract(price1.getClose());
	}

	public BigDecimal getDayChange() {
		List<IPrice> prices = adjustedPriceProvider.getPrices();
		if (prices.size() < 2) {
			return BigDecimal.ZERO;
		}

		IPrice price1 = prices.get(prices.size() - 2);
		IPrice price2 = prices.get(prices.size() - 1);

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
		FundamentalData currentFundamentalData = security.getCurrentFundamentalData();
		if (currentFundamentalData == null) {
			return null;
		}
		Currency currency = currentFundamentalData.getCurrency();
		ExchangeRates exchangeRate = Activator.getDefault().getExchangeRate();
		CurrencyConverter currencyConverter = exchangeRate.createCurrencyConverter(currency, security.getCurrency());
		if (currencyConverter == null) {
			return currentFundamentalData;
		}
		return new CurrencyAjustedFundamentalData(currentFundamentalData, currencyConverter);
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
		FundamentalData data1 = getCurrentFundamentalData();
		if (data1 != null) {
			peRatio = data1.calculatePeRatio(adjustedPriceProvider);
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

	public BigDecimal getPeDelta() {
		if (peDelta != null) {
			return peDelta;
		}
		FundamentalData d1 = getCurrentFundamentalData();
		AvgFundamentalData d2 = getAvgFundamentalData();
		if (d1 != null && d2 != null && d2.getAvgPE().signum() > 0) {
			BigDecimal v1 = d1.calculatePeRatio(getPriceProvider());
			BigDecimal v2 = d2.getAvgPE();
			peDelta = v1.divide(v2, MC).subtract(BigDecimal.ONE);
		}
		return peDelta;
	}

	public void refreshCache() {
		signal = null;
		avgPe = null;
		peDelta = null;
		peRatio = null;
	}
}