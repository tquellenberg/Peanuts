package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public class AdjustedPrices {
	
	private final static MathContext MC = new MathContext(10, RoundingMode.HALF_EVEN);

	public ImmutableList<IPrice> adjustPrices(List<? extends IPrice> prices, StockSplit split) {
		Builder<IPrice> builder = ImmutableList.builder();
		BigDecimal splitRatio = BigDecimal.ONE.divide(split.getRatio(), MC);
		for (IPrice price : prices) {
			if (price.getDay().before(split.getDay())) {
				builder.add(new AdjustedPrice(price, splitRatio));
			} else {
				builder.add(price);
			}
		}
		return builder.build();
	}
	
}
