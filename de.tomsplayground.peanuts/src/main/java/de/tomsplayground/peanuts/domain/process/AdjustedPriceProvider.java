package de.tomsplayground.peanuts.domain.process;

import java.util.Currency;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.domain.beans.PropertyChangeSupport;
import de.tomsplayground.peanuts.util.Day;

sealed public abstract class AdjustedPriceProvider extends ObservableModelObject implements IPriceProvider 
	permits CurrencyAdjustedPriceProvider, SplitAdjustedPriceProvider {

	private final IPriceProvider rawPriceProvider;

	abstract ImmutableList<IPrice> adjust(ImmutableList<IPrice> prices);
	abstract IPrice adjust(IPrice price);

	@Override
	public PropertyChangeSupport getPropertyChangeSupport() {
		return ((ObservableModelObject)rawPriceProvider).getPropertyChangeSupport();
	}
	
	public AdjustedPriceProvider(IPriceProvider rawPriceProvider) {
		this.rawPriceProvider = rawPriceProvider;
	}

	@Override
	public ImmutableList<IPrice> getPrices() {
		return adjust(rawPriceProvider.getPrices());
	}

	@Override
	public ImmutableList<IPrice> getPrices(Day from, Day to) {
		return adjust(rawPriceProvider.getPrices(from, to));
	}

	@Override
	public IPrice getPrice(Day date) {
		return adjust(rawPriceProvider.getPrice(date));
	}

	@Override
	public void setPrice(IPrice p) {
		throw new NotImplementedException("setPrice");
	}

	@Override
	public void setPrice(IPrice newPrice, boolean overideExistingData) {
		throw new NotImplementedException("setPrice");
	}

	@Override
	public boolean setPrices(List<? extends IPrice> prices, boolean overideExistingData) {
		throw new NotImplementedException("setPrice");
	}

	@Override
	public void removePrice(Day date) {
		throw new NotImplementedException("removePrice");
	}

	@Override
	public Day getMinDate() {
		return rawPriceProvider.getMinDate();
	}

	@Override
	public Day getMaxDate() {
		return rawPriceProvider.getMaxDate();
	}

	@Override
	public Security getSecurity() {
		return rawPriceProvider.getSecurity();
	}

	@Override
	public Currency getCurrency() {
		return rawPriceProvider.getCurrency();
	}

}
