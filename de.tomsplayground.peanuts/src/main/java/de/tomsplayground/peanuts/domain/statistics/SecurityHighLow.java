package de.tomsplayground.peanuts.domain.statistics;

import java.util.List;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.IPriceProviderFactory;
import de.tomsplayground.peanuts.domain.process.IStockSplitProvider;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.process.StockSplit;
import de.tomsplayground.peanuts.util.Day;

public class SecurityHighLow {

	private final IPriceProviderFactory priceProviderFactory;
	
	public record HighLowEntry(Security security, IPrice high, IPrice low) {
	}
	
	public SecurityHighLow(IPriceProviderFactory priceProviderFactory) {
		this.priceProviderFactory = priceProviderFactory;
	}
	
	public List<HighLowEntry> allHighLow(List<Security> securities, IStockSplitProvider stockSplitProvider) {
		return securities.parallelStream()
			.filter(s -> ! s.isDeleted())
			.map(s -> getHighLow(s, stockSplitProvider.getStockSplits(s)))
			.toList();
	}
	
	public HighLowEntry getHighLow(Security security, List<StockSplit> stockSplits) {
		IPriceProvider priceProvider = priceProviderFactory.getSplitAdjustedPriceProvider(security, stockSplits);

		Day today = Day.today();
		ImmutableList<IPrice> prices = priceProvider.getPrices(today.addYear(-1), today);
		if (prices.isEmpty()) {
			return new HighLowEntry(security, Price.ZERO, Price.ZERO);
		}

		IPrice low = prices.get(0);
		IPrice high = prices.get(0);
		for (IPrice iPrice : prices) {
			if (iPrice.getValue().compareTo(low.getValue()) < 0) {
				low = iPrice;
			}
			if (iPrice.getValue().compareTo(high.getValue()) > 0) {
				high = iPrice;
			}
		}
		
		return new HighLowEntry(security, high, low);
	}

}
