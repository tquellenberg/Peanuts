package de.tomsplayground.peanuts.client.watchlist;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.joda.time.DateTime;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.InventoryEntry;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;
import de.tomsplayground.peanuts.domain.currenncy.ExchangeRates;
import de.tomsplayground.peanuts.domain.fundamental.AvgFundamentalData;
import de.tomsplayground.peanuts.domain.fundamental.CurrencyAjustedFundamentalData;
import de.tomsplayground.peanuts.domain.fundamental.FundamentalData;
import de.tomsplayground.peanuts.domain.fundamental.FundamentalDatas;
import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.statistics.Signal;
import de.tomsplayground.peanuts.domain.statistics.SimpleMovingAverage;
import de.tomsplayground.util.Day;

public class WatchEntry {
	private static final BigDecimal TWO = new BigDecimal(2);

	private static final MathContext MC = new MathContext(10, RoundingMode.HALF_EVEN);

	private final Security security;
	private final IPriceProvider adjustedPriceProvider;

	// Cache
	Signal signal = null;
	BigDecimal avgPe = null;
	BigDecimal peDelta = null;
	BigDecimal peRatio = null;
	BigDecimal robustness = null;

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
		return adjust(security.getFundamentalDatas().getCurrentFundamentalData());
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
		FundamentalDatas fundamentalDatas = security.getFundamentalDatas();
		ExchangeRates exchangeRate = Activator.getDefault().getExchangeRate();
		return fundamentalDatas.getAvgFundamentalData(adjustedPriceProvider, exchangeRate);
	}

	public BigDecimal getPeRatio() {
		if (peRatio != null) {
			return peRatio;
		}
		Day date = new Day();
		IPrice price = adjustedPriceProvider.getPrice(date);
		FundamentalDatas fundamentalDatas = security.getFundamentalDatas();
		ExchangeRates exchangeRate = Activator.getDefault().getExchangeRate();
		BigDecimal earnings = fundamentalDatas.getAdjustedContinuousEarnings(date, exchangeRate);
		if (earnings != null) {
			peRatio = price.getClose().divide(earnings, MC);
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

	public BigDecimal getRobustness() {
		if (robustness != null) {
			return robustness;
		}
		AvgFundamentalData avgFundamentalData = getAvgFundamentalData();
		if (avgFundamentalData != null) {
			robustness = avgFundamentalData.getRobustness();
		}
		return robustness;
	}

	public BigDecimal getScore() {
		BigDecimal score = BigDecimal.ZERO;
		BigDecimal v = getPeDelta();
		if (v == null) {
			return null;
		}
		score = score.add(v.negate());

		v = getCurrencyAdjustedReturn();
		if (v == null) {
			return null;
		}
		score = score.add(v);

		v = getRobustness();
		if (v == null) {
			return null;
		}
		score = score.add(v.divide(TWO, MC));
		return score;
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

	public BigDecimal getCurrencyAdjustedReturn() {
		if (currencyAdjustedAvgEpsChange != null) {
			return currencyAdjustedAvgEpsChange;
		}
		AvgFundamentalData avgFundamentalData = getAvgFundamentalData();
		if (avgFundamentalData != null) {
			BigDecimal currencyAdjustedAvgEpsGrowth = avgFundamentalData.getCurrencyAdjustedAvgEpsGrowth();
			if (currencyAdjustedAvgEpsGrowth == null) {
				return null;
			}
			currencyAdjustedAvgEpsChange = currencyAdjustedAvgEpsGrowth.subtract(BigDecimal.ONE);
			BigDecimal divYield = getDivYield();
			if (divYield != null) {
				currencyAdjustedAvgEpsChange = currencyAdjustedAvgEpsChange.add(divYield);
			}
		}
		return currencyAdjustedAvgEpsChange;
	}

	public BigDecimal getPeDelta() {
		if (peDelta != null) {
			return peDelta;
		}
		BigDecimal v1 = getPeRatio();
		BigDecimal v2 = getAvgPE();
		if (v1 != null && v2 != null && v1.signum() > 0 && v2.signum() > 0) {
			peDelta = v1.subtract(v2).divide(v2, MC);
		} else {
			peDelta = null;
		}
		return peDelta;
	}

	public DateTime getFundamentalDataDate() {
		return security.getFundamentalDatas().getMaxModificationDate().orElse(null);
	}

	public void refreshCache() {
		signal = null;
		avgPe = null;
		peDelta = null;
		peRatio = null;
		robustness = null;
		currencyAdjustedAvgEpsChange = null;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).append(security.getName()).build();
	}
}