package de.tomsplayground.peanuts.domain.process;

import java.util.Currency;
import java.util.List;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.util.Day;


public interface IPriceProvider {

	Security getSecurity();

	Currency getCurrency();

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
