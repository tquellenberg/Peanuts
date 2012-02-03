package de.tomsplayground.peanuts.client.watchlist;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.statistics.Signal;
import de.tomsplayground.peanuts.domain.statistics.SimpleMovingAverage;
import de.tomsplayground.util.Day;

public class WatchEntry {
	private final Security security;
	private final IPriceProvider priceProvider;
	
	WatchEntry(Security security, IPriceProvider priceProvider) {
		this.security = security;
		this.priceProvider = priceProvider;
	}
	public IPriceProvider getPriceProvider() {
		return priceProvider;
	}
	public Price getPrice() {
		if (priceProvider.getMaxDate() != null)
			return priceProvider.getPrice(priceProvider.getMaxDate());
		return Price.ZERO;
	}
	public Security getSecurity() {
		return security;
	}
	public Signal getSignal() {
		SimpleMovingAverage simpleMovingAverage = new SimpleMovingAverage(20);
		simpleMovingAverage.calculate(priceProvider.getPrices());
		List<Signal> signals = simpleMovingAverage.getSignals();
		Signal signal = null;
		if (! signals.isEmpty()) {
			signal = signals.get(signals.size() -1);
		}
		return signal;
	}
	
	public BigDecimal getDayChangeAbsolut() {
		List<Price> prices = priceProvider.getPrices();
		if (prices.size() < 2)
			return BigDecimal.ZERO;
		
		Price price1 = prices.get(prices.size() - 2);
		Price price2 = prices.get(prices.size() - 1);
		
		return price2.getClose().subtract(price1.getClose());
	}
	
	public BigDecimal getDayChange() {
		List<Price> prices = priceProvider.getPrices();
		if (prices.size() < 2)
			return BigDecimal.ZERO;
		
		Price price1 = prices.get(prices.size() - 2);
		Price price2 = prices.get(prices.size() - 1);
		
		if (price1.getClose().signum() == 0)
			return BigDecimal.ZERO;
		
		BigDecimal delta = price2.getClose().subtract(price1.getClose());
		
		BigDecimal performance = delta.divide(price1.getClose(), new MathContext(10, RoundingMode.HALF_EVEN));
		return performance;
	}
	
	public BigDecimal getPerformance(int day, int month, int year) {
		Day maxDate = priceProvider.getMaxDate();
		if (maxDate != null) {
			Day minDay = maxDate.addDays(-day);
			minDay = minDay.addMonth(-month);
			minDay = minDay.addYear(-year);
			
			Price price1 = priceProvider.getPrice(minDay);
			Price price2 = priceProvider.getPrice(maxDate);
			
			BigDecimal delta = price2.getClose().subtract(price1.getClose());
			
			if (price1.getClose().signum() == 0) {
				return BigDecimal.ZERO;
			}
			
			BigDecimal performance = delta.divide(price1.getClose(), new MathContext(10, RoundingMode.HALF_EVEN));
			return performance;
		}
		return BigDecimal.ZERO;
	}
}