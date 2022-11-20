package de.tomsplayground.peanuts.domain.process;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.base.Security;

public interface IStockSplitProvider {
	
	ImmutableList<StockSplit> getStockSplits(Security security);

}
