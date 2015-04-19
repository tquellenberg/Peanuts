package de.tomsplayground.peanuts.domain.process;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class SplitAdjustedPriceProvider extends AdjustedPriceProvider {

	private final ImmutableList<StockSplit> stockSplits;

	public SplitAdjustedPriceProvider(IPriceProvider rawPriceProvider, List<StockSplit> stockSplits) {
		super(rawPriceProvider);
		this.stockSplits = ImmutableList.copyOf(stockSplits);
	}

	@Override
	ImmutableList<IPrice> adjust(ImmutableList<IPrice> prices) {
		SplitAdjustedPrices splitAdjustedPrices = new SplitAdjustedPrices();
		for (StockSplit split : stockSplits) {
			prices = splitAdjustedPrices.adjustPrices(prices, split);
		}
		return prices;
	}

}
