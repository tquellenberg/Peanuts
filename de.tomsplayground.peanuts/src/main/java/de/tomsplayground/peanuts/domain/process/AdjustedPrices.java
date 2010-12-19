package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class AdjustedPrices {
	
	private final static MathContext MC = new MathContext(10, RoundingMode.HALF_EVEN);

	public List<IPrice> adjustPrices(List<? extends IPrice> prices, StockSplit split) {
		List<IPrice> result = new ArrayList<IPrice>();
		BigDecimal ratio = BigDecimal.ONE;
		BigDecimal splitRatio = BigDecimal.ONE.divide(split.getRatio(), MC);
		boolean splitSet = false;
		for (int i = prices.size() - 1 ; i >= 0; i--) {
			IPrice price = prices.get(i);
			if (!splitSet && price.getDay().before(split.getDay())) {
				ratio = splitRatio;
				splitSet = true;
			}
			result.add(new AdjustedPrice(price, ratio));
		}
		return result;
	}
	
}
