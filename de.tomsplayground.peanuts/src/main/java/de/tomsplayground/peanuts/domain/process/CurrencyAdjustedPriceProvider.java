package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;

public class CurrencyAdjustedPriceProvider extends AdjustedPriceProvider {

	private final CurrencyConverter currencyConverter;

	public CurrencyAdjustedPriceProvider(IPriceProvider priceProvider, CurrencyConverter currencyConverter) {
		super(priceProvider);
		this.currencyConverter = currencyConverter;
	}

	@Override
	ImmutableList<IPrice> adjust(ImmutableList<IPrice> prices) {
		Builder<IPrice> builder = ImmutableList.builder();
		for (IPrice price : prices) {
			BigDecimal ratio = currencyConverter.getRatio(price.getDay());
			builder.add(new AdjustedPrice(price, ratio));
		}
		return builder.build();
	}

}
