package de.tomsplayground.peanuts.domain.process;

import java.util.Currency;
import java.util.List;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;
import de.tomsplayground.peanuts.domain.currenncy.ExchangeRates;

public class CurrencyAdjustedPriceProviderFactory implements IPriceProviderFactory {

	private final Currency currency;
	private final IPriceProviderFactory priceProviderFactory;
	private final ExchangeRates exchangeRates;

	public CurrencyAdjustedPriceProviderFactory(Currency currency, IPriceProviderFactory priceProviderFactory, ExchangeRates exchangeRates) {
		this.currency = currency;
		this.priceProviderFactory = priceProviderFactory;
		this.exchangeRates = exchangeRates;
	}

	@Override
	public IPriceProvider getPriceProvider(Security security) {
		IPriceProvider priceProvider = priceProviderFactory.getPriceProvider(security);
		priceProvider = warpPriceProvider(priceProvider, security.getCurrency());
		return priceProvider;
	}

	@Override
	public IPriceProvider getSplitAdjustedPriceProvider(Security security, List<StockSplit> stockSplits) {
		IPriceProvider priceProvider = priceProviderFactory.getSplitAdjustedPriceProvider(security, stockSplits);
		priceProvider = warpPriceProvider(priceProvider, security.getCurrency());
		return priceProvider;
	}

	private IPriceProvider warpPriceProvider(IPriceProvider priceProvider, Currency currency2) {
		if (! currency2.equals(currency)) {
			CurrencyConverter currencyConverter = exchangeRates.createCurrencyConverter(currency2, currency);
			return new CurrencyAdjustedPriceProvider(priceProvider, currencyConverter);
		}
		return priceProvider;
	}

}
