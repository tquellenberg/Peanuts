package de.tomsplayground.peanuts.client.watchlist;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.statistics.Signal;
import de.tomsplayground.peanuts.domain.statistics.SimpleMovingAverage;
import de.tomsplayground.util.Day;

public class WatchEntry {
	private final Security security;
	private final IPriceProvider priceProvider;
	private final IPriceProvider adjustedPriceProvider;
	
	WatchEntry(Security security, IPriceProvider priceProvider, IPriceProvider adjustedPriceProvider) {
		this.security = security;
		this.priceProvider = priceProvider;
		this.adjustedPriceProvider = adjustedPriceProvider;
	}
	public IPriceProvider getPriceProvider() {
		return priceProvider;
	}
	public IPrice getPrice() {
		if (priceProvider.getMaxDate() != null) {
			return priceProvider.getPrice(priceProvider.getMaxDate());
		}
		return Price.ZERO;
	}
	public Security getSecurity() {
		return security;
	}
	public Signal getSignal() {
		SimpleMovingAverage simpleMovingAverage = new SimpleMovingAverage(20);
		simpleMovingAverage.calculate(adjustedPriceProvider.getPrices());
		List<Signal> signals = simpleMovingAverage.getSignals();
		Signal signal = null;
		if (! signals.isEmpty()) {
			signal = signals.get(signals.size() -1);
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
		
		BigDecimal performance = delta.divide(price1.getClose(), new MathContext(10, RoundingMode.HALF_EVEN));
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
		BigDecimal performance = delta.divide(price1.getClose(), new MathContext(10, RoundingMode.HALF_EVEN));
		return performance;
	}
}