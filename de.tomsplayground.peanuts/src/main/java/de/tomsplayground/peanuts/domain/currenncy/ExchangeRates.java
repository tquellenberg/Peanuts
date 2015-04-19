package de.tomsplayground.peanuts.domain.currenncy;

import java.util.Currency;

import de.tomsplayground.peanuts.domain.base.ISecurityProvider;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.IPriceProviderFactory;

public class ExchangeRates {

	private final IPriceProviderFactory priceProviderFactory;
	private final ISecurityProvider securityProvider;

	public ExchangeRates(IPriceProviderFactory priceProviderFactory, ISecurityProvider securityProvider) {
		this.priceProviderFactory = priceProviderFactory;
		this.securityProvider = securityProvider;
	}

	public CurrencyConverter createCurrencyConverter(Currency from, Currency to) {
		for (Security s : securityProvider.getSecurities()) {
			if (s.getExchangeCurrency() != null) {
				if (s.getCurrency().equals(from) && s.getExchangeCurrency().equals(to)) {
					IPriceProvider priceProvider = priceProviderFactory.getPriceProvider(s);
					return new CurrencyConverter(priceProvider, from, to);
				}
				if (s.getCurrency().equals(to) && s.getExchangeCurrency().equals(from)) {
					IPriceProvider priceProvider = priceProviderFactory.getPriceProvider(s);
					return new CurrencyConverter(priceProvider, from, to, true);
				}
			}
		}
		return null;
	}

}
