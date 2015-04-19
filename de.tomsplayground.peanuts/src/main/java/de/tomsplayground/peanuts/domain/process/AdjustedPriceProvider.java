package de.tomsplayground.peanuts.domain.process;

import java.util.List;

import org.apache.commons.lang3.NotImplementedException;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.util.Day;

public abstract class AdjustedPriceProvider extends ObservableModelObject implements IPriceProvider {

	private final IPriceProvider rawPriceProvider;

	public AdjustedPriceProvider(IPriceProvider rawPriceProvider) {
		this.rawPriceProvider = rawPriceProvider;
	}

	@Override
	public String getName() {
		return rawPriceProvider.getName();
	}

	@Override
	public ImmutableList<IPrice> getPrices() {
		return adjust(rawPriceProvider.getPrices());
	}

	abstract ImmutableList<IPrice> adjust(ImmutableList<IPrice> prices);

	@Override
	public ImmutableList<IPrice> getPrices(Day from, Day to) {
		return adjust(rawPriceProvider.getPrices(from, to));
	}

	@Override
	public IPrice getPrice(Day date) {
		return adjust(ImmutableList.of(rawPriceProvider.getPrice(date))).get(0);
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
	public void setPrices(List<? extends IPrice> prices, boolean overideExistingData) {
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

	public IPriceProvider getRawPriceProvider() {
		return rawPriceProvider;
	}

}
