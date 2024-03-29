package de.tomsplayground.peanuts.client.watchlist;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import de.tomsplayground.peanuts.app.yahoo.MarketCap;
import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.editors.security.FundamentalDataEditorPart;
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
import de.tomsplayground.peanuts.domain.statistics.Volatility;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class WatchEntry {

	private static final BigDecimal TWO = new BigDecimal(2);

	private final Security security;
	private final IPriceProvider adjustedPriceProvider;

	// Cache
	BigDecimal avgPe = null;
	BigDecimal peDelta = null;
	BigDecimal peRatio = null;
	BigDecimal robustness = null;
	BigDecimal volatility = null;
	BigDecimal marketCap = null;

	private BigDecimal currencyAdjustedAvgReturnGrowth;

	WatchEntry(Security security, IPriceProvider adjustedPriceProvider) {
		if (! security.getCurrency().equals(adjustedPriceProvider.getCurrency())) {
			throw new IllegalArgumentException("Security and price provide must use same currency. ("+security.getCurrency()+", "+adjustedPriceProvider.getCurrency()+")");
		}
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

	public BigDecimal getDayChangeAbsolut() {
		Day now = Day.today();
		IPrice price1 = adjustedPriceProvider.getPrice(now.addDays(-1).adjustWorkday());
		IPrice price2 = adjustedPriceProvider.getPrice(now);

		return price2.getValue().subtract(price1.getValue());
	}

	public BigDecimal getDayChange() {
		Day now = Day.today();
		IPrice price1 = adjustedPriceProvider.getPrice(now.addDays(-1).adjustWorkday());
		IPrice price2 = adjustedPriceProvider.getPrice(now);

		if (price1.getValue().signum() == 0) {
			return BigDecimal.ZERO;
		}

		BigDecimal delta = price2.getValue().subtract(price1.getValue());

		BigDecimal performance = delta.divide(price1.getValue(), PeanutsUtil.MC);
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
		BigDecimal delta = price2.getValue().subtract(price1.getValue());
		if (price1.getValue().signum() == 0) {
			return BigDecimal.ZERO;
		}
		BigDecimal performance = delta.divide(price1.getValue(), PeanutsUtil.MC);
		return performance;
	}

	private CurrencyAjustedFundamentalData getCurrentFundamentalData() {
		return adjust(security.getFundamentalDatas().getCurrentFundamentalData());
	}

	private CurrencyAjustedFundamentalData adjust(FundamentalData fundamentalData) {
		if (fundamentalData == null) {
			return null;
		}
		Currency currency = fundamentalData.getCurrency();
		ExchangeRates exchangeRates = Activator.getDefault().getExchangeRates();
		CurrencyConverter currencyConverter = exchangeRates.createCurrencyConverter(currency, security.getCurrency());
		return new CurrencyAjustedFundamentalData(fundamentalData, currencyConverter);
	}

	private AvgFundamentalData getAvgFundamentalData() {
		FundamentalDatas fundamentalDatas = security.getFundamentalDatas();
		ExchangeRates exchangeRates = Activator.getDefault().getExchangeRates();
		return fundamentalDatas.getAvgFundamentalData(adjustedPriceProvider, exchangeRates);
	}

	public BigDecimal getPeRatio() {
		if (peRatio != null) {
			return peRatio;
		}
		Day date = Day.today();
		IPrice price = adjustedPriceProvider.getPrice(date);
		FundamentalDatas fundamentalDatas = security.getFundamentalDatas();
		ExchangeRates exchangeRates = Activator.getDefault().getExchangeRates();
		BigDecimal earnings = fundamentalDatas.getAdjustedContinuousEarnings(date, adjustedPriceProvider.getCurrency(), exchangeRates);
		if (earnings != null && earnings.signum() > 0) {
			peRatio = price.getValue().divide(earnings, PeanutsUtil.MC);
		}
		return peRatio;
	}

	public BigDecimal getDivYield() {
		CurrencyAjustedFundamentalData data1 = getCurrentFundamentalData();
		if (data1 != null) {
			return data1.calculateDivYield(adjustedPriceProvider);
		}
		return null;
	}

	public BigDecimal getDebtEquityRatio() {
		CurrencyAjustedFundamentalData data1 = getCurrentFundamentalData();
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

		v = getCurrencyAdjustedAvgReturnGrowth();
		if (v == null) {
			return null;
		}
		score = score.add(v);

		v = getRobustness();
		if (v == null) {
			return null;
		}
		score = score.add(v.divide(TWO, PeanutsUtil.MC));
		return score;
	}

	public BigDecimal getAvgPE() {
		if (avgPe != null) {
			return avgPe;
		}

		avgPe = security.getFundamentalDatas().getOverriddenAvgPE();
		if (avgPe == null) {
			AvgFundamentalData avgFundamentalData = getAvgFundamentalData();
			if (avgFundamentalData != null) {
				avgPe = avgFundamentalData.getAvgPE();
			}
		}
		return avgPe;
	}

	/**
	 * Adjusted to currency of security.
	 */
	public BigDecimal getCurrencyAdjustedAvgReturnGrowth() {
		if (currencyAdjustedAvgReturnGrowth != null) {
			return currencyAdjustedAvgReturnGrowth;
		}
		AvgFundamentalData avgFundamentalData = getAvgFundamentalData();
		if (avgFundamentalData != null) {
			BigDecimal currencyAdjustedAvgEpsGrowth = avgFundamentalData.getCurrencyAdjustedAvgEpsGrowth();
			if (currencyAdjustedAvgEpsGrowth == null) {
				return null;
			}
			currencyAdjustedAvgReturnGrowth = currencyAdjustedAvgEpsGrowth.subtract(BigDecimal.ONE);
			BigDecimal divYield = getDivYield();
			if (divYield != null) {
				currencyAdjustedAvgReturnGrowth = currencyAdjustedAvgReturnGrowth.add(divYield);
			}
		}
		return currencyAdjustedAvgReturnGrowth;
	}

	public BigDecimal getPeDelta() {
		if (peDelta != null) {
			return peDelta;
		}
		BigDecimal v1 = getPeRatio();
		BigDecimal v2 = getAvgPE();
		if (v1 != null && v2 != null && v1.signum() > 0 && v2.signum() > 0) {
			peDelta = v1.subtract(v2).divide(v2, PeanutsUtil.MC);
		} else {
			peDelta = null;
		}
		return peDelta;
	}

	public BigDecimal getDeRatio() {
		BigDecimal deRatio = BigDecimal.ZERO;
		FundamentalDatas fundamentalDatas = security.getFundamentalDatas();
		for (FundamentalData data : fundamentalDatas.getDatas()) {
			if (data.getDebtEquityRatio().compareTo(BigDecimal.ZERO) > 0) {
				deRatio = data.getDebtEquityRatio();
			}
		}
		return deRatio;
	}

	public LocalDateTime getFundamentalDataDate() {
		return security.getFundamentalDatas().getMaxModificationDate().orElse(null);
	}

	public void refreshCache() {
		avgPe = null;
		peDelta = null;
		peRatio = null;
		robustness = null;
		volatility = null;
		currencyAdjustedAvgReturnGrowth = null;
		marketCap = null;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).append(security.getName()).build();
	}

	public BigDecimal getVolatility() {
		if (volatility == null) {
			double vola = new Volatility().calculateVolatility(adjustedPriceProvider);
			volatility = new BigDecimal(vola);
		}
		return volatility;
	}

	public BigDecimal getMarketCap() {
		if (marketCap == null) {
			String currency = security.getConfigurationValue(FundamentalDataEditorPart.SECURITY_MARKET_CAP_CURRENCY);
			String marketCapValueStr = security.getConfigurationValue(FundamentalDataEditorPart.SECURITY_MARKET_CAP_VALUE);
			if (StringUtils.isNotEmpty(currency) && StringUtils.isNotEmpty(marketCapValueStr)) {
				marketCap = new MarketCap(new BigDecimal(marketCapValueStr), currency).getMarketCapInDefaultCurrency(Activator.getDefault().getExchangeRates());
			} else {
				marketCap = BigDecimal.ZERO;
			}
		}
		return marketCap;
	}
}