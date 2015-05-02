package de.tomsplayground.peanuts.domain.process;

import java.util.List;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.util.Day;


public interface IPriceProvider {

	String getName();

	Security getSecurity();

	ImmutableList<IPrice> getPrices();

	ImmutableList<IPrice> getPrices(Day from, Day to);

	IPrice getPrice(Day date);

	void setPrice(IPrice p);

	void setPrice(IPrice newPrice, boolean overideExistingData);

	void setPrices(List<? extends IPrice> prices, boolean overideExistingData);

	void removePrice(Day date);

	Day getMinDate();

	Day getMaxDate();

}
