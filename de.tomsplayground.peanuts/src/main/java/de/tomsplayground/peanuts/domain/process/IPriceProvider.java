package de.tomsplayground.peanuts.domain.process;

import java.util.List;

import de.tomsplayground.util.Day;


public interface IPriceProvider {

	String getName();

	List<Price> getPrices();

	List<Price> getPrices(Day from, Day to);
	
	Price getPrice(Day date);
	
	void setPrice(Price p);
	
	void setPrice(Price newPrice, boolean updateExistingPrice);

	void setPrices(List<Price> prices, boolean updateExistingPrice);
	
	void removePrice(Day date);

	Day getMinDate();

	Day getMaxDate();

}
