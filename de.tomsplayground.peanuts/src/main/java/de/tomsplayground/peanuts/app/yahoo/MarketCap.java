package de.tomsplayground.peanuts.app.yahoo;

import java.math.BigDecimal;
import java.util.Currency;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tomsplayground.peanuts.domain.currenncy.Currencies;
import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;
import de.tomsplayground.peanuts.domain.currenncy.ExchangeRates;
import de.tomsplayground.peanuts.util.Day;

public class MarketCap {

	private final static Logger log = LoggerFactory.getLogger(MarketCap.class);

	private final BigDecimal marketCap;
	private final Currency currency;

	public MarketCap(BigDecimal marketCap, String currency) {
		this.marketCap = marketCap;
		Currency c = null;
		// Market cap is always in main currency
		if (currency.equals("ZAc")) {
			currency = "ZAR";
		}
		try {
			c = Currency.getInstance(currency.toUpperCase());
		} catch (IllegalArgumentException e) {
			log.error("Unknown currency: {}", currency);
		}
		this.currency = c;
	}

	public Currency getCurrency() {
		return currency;
	}

	public BigDecimal getMarketCap() {
		return marketCap;
	}

	public BigDecimal getMarketCapInDefaultCurrency(ExchangeRates exchangeRates) {
		CurrencyConverter currencyConverter = exchangeRates.createCurrencyConverter(currency, Currencies.getInstance().getDefaultCurrency());
		if (currencyConverter != null) {
			return currencyConverter.convert(marketCap, Day.today());
		} else {
			log.error("No currency converter available for {}", currency);
			return BigDecimal.ZERO;
		}
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}