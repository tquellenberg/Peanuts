package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.util.PeanutsUtil;
import de.tomsplayground.util.Day;


public abstract class PriceProvider extends ObservableModelObject implements IPriceProvider {

	private ImmutableList<Price> prices = ImmutableList.of();
	
	public PriceProvider() {
		super();
	}

	@Override
	public Day getMaxDate() {
		ImmutableList<Price> lp = prices;
		if (lp.isEmpty())
			return null;
		return lp.get(lp.size() - 1).getDay();
	}

	@Override
	public Day getMinDate() {
		ImmutableList<Price> lp = prices;
		if (lp.isEmpty())
			return null;
		return lp.get(0).getDay();
	}

	@Override
	public ImmutableList<Price> getPrices() {
		return prices;
	}

	@Override
	public ImmutableList<Price> getPrices(Day from, Day to) {
		if (from.after(to)) {
			throw new IllegalArgumentException("'from' after 'to'" + from + "->" + to);
		}
		int start = 0;
		int end = 0;
		ImmutableList<Price> lp = prices;
		for (Price p : lp) {
			if (p.getDay().compareTo(from) < 0) {
				start++ ;
			}
			if (p.getDay().compareTo(to) <= 0) {
				end++ ;
			}
		}
		return lp.subList(start, end);
	}

	@Override
	public Price getPrice(Day date) {
		ImmutableList<Price> lp = prices;
		if (lp.isEmpty())
			return new Price(date, BigDecimal.ZERO);
		int pos = PeanutsUtil.binarySearch(lp, date);
		if (pos >= 0) {
			return lp.get(pos);
		} else {
			pos = -pos - 1;
			if (pos >= lp.size())
				pos --;
			return lp.get(pos);
		}
	}
	
	@Override
	public void setPrice(Price newPrice) {
		setPrice(newPrice, false);
	}

	@Override
	public void setPrice(Price newPrice, boolean updateExistingPrice) {
		Price oldPrice = setPriceInternal(newPrice, updateExistingPrice);
		firePropertyChange("prices", oldPrice, newPrice);
	}
	
	@Override
	public void setPrices(List<Price> prices, boolean updateExistingPrice) {
		boolean change = false;
		for (Price price : prices) {
			Price oldValue = setPriceInternal(price, updateExistingPrice);
			change = change || oldValue == null || ! oldValue.equals(price);
		}
		if (change)
			firePropertyChange("prices", null, prices);
	}
	
	public Price setPriceInternal(Price newPrice, boolean updateExistingPrice) {
		ImmutableList<Price> lp = prices;
		
		int binarySearch = PeanutsUtil.binarySearch(lp, newPrice.getDay());
		int s1, s2;
		Price oldPrice = null;
		if (binarySearch >= 0) {
			oldPrice = lp.get(binarySearch);
			if (updateExistingPrice) {
				// Update existing price with better values
				BigDecimal open = newPrice.getOpen() != null ? newPrice.getOpen() : oldPrice.getOpen();
				BigDecimal close = newPrice.getClose() != null ? newPrice.getClose() : oldPrice.getClose();
				BigDecimal high = newPrice.getHigh() != null ? newPrice.getHigh() : oldPrice.getHigh();
				BigDecimal low = newPrice.getLow() != null ? newPrice.getLow() : oldPrice.getLow();
				newPrice = new Price(oldPrice.getDay(), open, close, high, low);				
			}
			s1 = binarySearch;
			s2 = binarySearch +1;
		} else {
			s1 = s2 = -binarySearch -1;
		}

		ImmutableList<Price> subList1 = lp.subList(0, s1);
		ImmutableList<Price> subList2 = lp.subList(s2, lp.size());
		prices = ImmutableList.copyOf(Iterables.concat(
			subList1,
			ImmutableList.of(newPrice),
			subList2));
		
		return oldPrice;
	}

	@Override
	public void removePrice(final Day date) {
		final Price[] removed = new Price[1];
		ImmutableList<Price> lp = ImmutableList.copyOf(
			Iterables.filter(prices, new Predicate<Price>() {
				@Override
				public boolean apply(Price input) {
					if (input.getDay().equals(date)) {
						removed[0] = input;
						return false;
					}
					return true;
				}
			})
		);
		if (removed[0] != null) {
			prices = lp;
			firePropertyChange("prices", removed[0], null);
		}
	}
}