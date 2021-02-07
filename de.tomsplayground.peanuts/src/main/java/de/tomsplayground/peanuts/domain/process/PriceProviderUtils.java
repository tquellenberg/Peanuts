package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.util.PeanutsUtil;
import de.tomsplayground.util.Day;

public class PriceProviderUtils {

	public static BigDecimal avg(IPriceProvider priceProvider, Day from, Day to) {
		ImmutableList<IPrice> prices = priceProvider.getPrices(from, to);
		if (prices.isEmpty()) {
			if (priceProvider.getMaxDate() != null) {
				return priceProvider.getPrice(priceProvider.getMaxDate()).getClose();
			} else {
				return BigDecimal.ZERO;
			}
		}
		BigDecimal sum = BigDecimal.ZERO;
		for (IPrice p : prices) {
			sum = sum.add(p.getClose());
		}
		return sum.divide(new BigDecimal(prices.size()), PeanutsUtil.MC);
	}
}
