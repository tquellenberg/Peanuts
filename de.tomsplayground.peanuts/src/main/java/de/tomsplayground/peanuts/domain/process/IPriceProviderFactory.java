package de.tomsplayground.peanuts.domain.process;

import java.util.List;

import de.tomsplayground.peanuts.domain.base.Security;

public interface IPriceProviderFactory {

	IPriceProvider getPriceProvider(Security security);

	IPriceProvider getSplitAdjustedPriceProvider(Security security, List<StockSplit> stockSplits);
}