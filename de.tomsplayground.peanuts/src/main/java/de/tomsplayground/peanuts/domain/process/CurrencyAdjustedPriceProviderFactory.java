package de.tomsplayground.peanuts.domain.process;

import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;
import de.tomsplayground.peanuts.domain.currenncy.ExchangeRates;

public class CurrencyAdjustedPriceProviderFactory implements IPriceProviderFactory {

	private final Currency currency;
	private final IPriceProviderFactory plainPriceProviderFactory;
	private final ExchangeRates exchangeRates;
	
	private final Map<IPriceProvider, IPriceProvider> cache = new HashMap<>();

	public CurrencyAdjustedPriceProviderFactory(Currency currency, IPriceProviderFactory plainPriceProviderFactory, ExchangeRates exchangeRates) {
		this.currency = currency;
		this.plainPriceProviderFactory = plainPriceProviderFactory;
		this.exchangeRates = exchangeRates;
	}

	@Override
	public IPriceProvider getPriceProvider(Security security) {
		IPriceProvider priceProvider = plainPriceProviderFactory.getPriceProvider(security);
		priceProvider = wrapPriceProvider(priceProvider);
		return priceProvider;
	}

	@Override
	public IPriceProvider getSplitAdjustedPriceProvider(Security security, List<StockSplit> stockSplits) {
		IPriceProvider priceProvider = plainPriceProviderFactory.getSplitAdjustedPriceProvider(security, stockSplits);
		priceProvider = wrapPriceProvider(priceProvider);
		return priceProvider;
	}

	private IPriceProvider wrapPriceProvider(IPriceProvider originalPriceProvider) {
		if (! originalPriceProvider.getCurrency().equals(currency)) {
			synchronized (cache) {
				IPriceProvider result = cache.get(originalPriceProvider);
				if (result == null) {
					CurrencyConverter currencyConverter = exchangeRates.createCurrencyConverter(originalPriceProvider.getCurrency(), currency);
					result = new CurrencyAdjustedPriceProvider(originalPriceProvider, currencyConverter);
					cache.put(originalPriceProvider, result);
				}
				return result;
			}
		}
		return originalPriceProvider;
	}

}
