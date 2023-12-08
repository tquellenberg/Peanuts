package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public final class SplitAdjustedPriceProvider extends AdjustedPriceProvider {

	private final ImmutableList<StockSplit> stockSplits;

	public SplitAdjustedPriceProvider(IPriceProvider rawPriceProvider, List<StockSplit> stockSplits) {
		super(rawPriceProvider);
		this.stockSplits = ImmutableList.copyOf(stockSplits);
	}

	@Override
	ImmutableList<IPrice> adjust(ImmutableList<IPrice> prices) {
		return prices.parallelStream()
			.map(p -> adjust(p))
			.collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
	}

	@Override
	IPrice adjust(IPrice price) {
		BigDecimal splitRatio = getSplitRatio(price.getDay());
		if (splitRatio.compareTo(BigDecimal.ONE) != 0) {
			return new Price(price.getDay(), price.getValue().multiply(splitRatio, PeanutsUtil.MC));
		}
		return price;
	}

	private BigDecimal getSplitRatio(Day day) {
		return stockSplits.stream()
			.filter(sp -> day.before(sp.getDay()))
			.map(sp -> sp.getRatio())
			.reduce(BigDecimal.ONE, BigDecimal::multiply);
	}
}
