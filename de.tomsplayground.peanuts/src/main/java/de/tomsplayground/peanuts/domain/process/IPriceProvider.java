package de.tomsplayground.peanuts.domain.process;

import java.util.List;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.util.Day;


public interface IPriceProvider {

	String getName();

	ImmutableList<Price> getPrices();

	ImmutableList<Price> getPrices(Day from, Day to);
	
	Price getPrice(Day date);
	
	void setPrice(Price p);
	
	void setPrice(Price newPrice, boolean overideExistingData);

	void setPrices(List<Price> prices, boolean overideExistingData);
	
	void removePrice(Day date);

	Day getMinDate();

	Day getMaxDate();

}
