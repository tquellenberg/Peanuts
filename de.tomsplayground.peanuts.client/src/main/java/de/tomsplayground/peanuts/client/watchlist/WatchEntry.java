package de.tomsplayground.peanuts.client.watchlist;

import java.math.BigDecimal;
import java.util.Currency;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.editors.security.properties.IvrPropertyPage;
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
import de.tomsplayground.peanuts.domain.statistics.Volatility;
import de.tomsplayground.peanuts.util.PeanutsUtil;
import de.tomsplayground.util.Day;

public class WatchEntry {

	private final static Logger log = LoggerFactory.getLogger(WatchEntry.class);

	private static final BigDecimal TWO = new BigDecimal(2);

	private final Security security;
	private final IPriceProvider adjustedPriceProvider;

	// Cache
	BigDecimal avgPe = null;
	BigDecimal peDelta = null;
	BigDecimal peRatio = null;
	BigDecimal robustness = null;
	BigDecimal volatility = null;

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

		BigDecimal performance = delta.divide(price1.getClose(), PeanutsUtil.MC);
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
		BigDecimal performance = delta.divide(price1.getClose(), PeanutsUtil.MC);
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
		if (earnings != null && earnings.signum() > 0) {
			peRatio = price.getClose().divide(earnings, PeanutsUtil.MC);
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
		score = score.add(v.divide(TWO, PeanutsUtil.MC));
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

	public DateTime getFundamentalDataDate() {
		return security.getFundamentalDatas().getMaxModificationDate().orElse(null);
	}

	public void refreshCache() {
		avgPe = null;
		peDelta = null;
		peRatio = null;
		robustness = null;
		volatility = null;
		currencyAdjustedAvgEpsChange = null;
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

	public BigDecimal getIvr() {
		String ivrValueStr = security.getConfigurationValue(IvrPropertyPage.IVR_RANK);
		if (StringUtils.isNotBlank(ivrValueStr)) {
			try {
				return new BigDecimal(ivrValueStr);
			} catch (NumberFormatException e) {
				log.error("IVR value: '"+ivrValueStr+"' for "+security, e);
			}
		}
		return null;
	}
}