package de.tomsplayground.peanuts.domain.process;

import java.util.Currency;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;

public final class CurrencyAdjustedPriceProvider extends AdjustedPriceProvider {

	private final CurrencyConverter currencyConverter;

	public CurrencyAdjustedPriceProvider(IPriceProvider priceProvider, CurrencyConverter currencyConverter) {
		super(priceProvider);
		this.currencyConverter = currencyConverter;
		if (! priceProvider.getCurrency().equals(currencyConverter.getFromCurrency())) {
			throw new IllegalArgumentException("Price provide and converter must use same currency. ("+priceProvider.getCurrency()+", "+currencyConverter.getFromCurrency()+")");
		}
	}

	@Override
	ImmutableList<IPrice> adjust(ImmutableList<IPrice> prices) {
		return prices.parallelStream()
			.map(p -> currencyConverter.convert(p))
			.collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
	}

	@Override
	IPrice adjust(IPrice price) {
		return currencyConverter.convert(price);
	}

	@Override
	public Currency getCurrency() {
		return currencyConverter.getToCurrency();
	}

}
