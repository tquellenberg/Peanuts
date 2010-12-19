package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.util.PeanutsUtil;
import de.tomsplayground.util.Day;


public abstract class PriceProvider extends ObservableModelObject implements IPriceProvider {

	final private List<Price> prices = new ArrayList<Price>();
	
	public PriceProvider() {
		super();
	}

	@Override
	public Day getMaxDate() {
		synchronized (prices) {
			if (prices.isEmpty())
				return null;
			return prices.get(prices.size() - 1).getDay();
		}
	}

	@Override
	public Day getMinDate() {
		synchronized (prices) {
			if (prices.isEmpty())
				return null;
			return prices.get(0).getDay();
		}
	}

	@Override
	public List<Price> getPrices() {
		synchronized (prices) {
			return new ArrayList<Price>(prices);
		}
	}

	@Override
	public List<Price> getPrices(Day from, Day to) {
		if (from.after(to)) {
			throw new IllegalArgumentException("'from' after 'to'" + from + "->" + to);
		}
		int start = 0;
		int end = 0;
		synchronized (prices) {
			for (Price p : prices) {
				if (p.getDay().compareTo(from) < 0) {
					start++ ;
				}
				if (p.getDay().compareTo(to) <= 0) {
					end++ ;
				}
			}
			return new ArrayList<Price>(prices.subList(start, end));
		}
	}

	@Override
	public Price getPrice(Day date) {
		synchronized (prices) {
			if (prices.isEmpty())
				return new Price(date, BigDecimal.ZERO);
			int pos = PeanutsUtil.binarySearch(prices, date);
			if (pos >= 0) {
				return prices.get(pos);
			} else {
				pos = -pos - 1;
				if (pos >= prices.size())
					pos --;
				return prices.get(pos);
			}
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
		Day newPriceDate = newPrice.getDay();
		synchronized (prices) {
			ListIterator<Price> iterator = prices.listIterator();
			while (iterator.hasNext()) {
				Price p = iterator.next();
				int compareTo = p.getDay().compareTo(newPriceDate);
				if (compareTo == 0) {
					if (! p.equals(newPrice)) {
						if (updateExistingPrice) {
							// Update existing price with better values
							BigDecimal open = newPrice.getOpen() != null ? newPrice.getOpen() : p.getOpen();
							BigDecimal close = newPrice.getClose() != null ? newPrice.getClose() : p.getClose();
							BigDecimal high = newPrice.getHigh() != null ? newPrice.getHigh() : p.getHigh();
							BigDecimal low = newPrice.getLow() != null ? newPrice.getLow() : p.getLow();
							newPrice = new Price(p.getDay(), open, close, high, low);
							iterator.set(newPrice);
						} else {
							// replace existing
							iterator.set(newPrice);
						}
					}
					return p;
				}
				if (compareTo > 0) {
					// insert in between
					iterator.previous();
					iterator.add(newPrice);
					return null;
				}
			}
			// add to end
			prices.add(newPrice);
			return null;
		}
	}

	@Override
	public void removePrice(Day date) {
		synchronized (prices) {
			ListIterator<Price> iterator = prices.listIterator();
			while (iterator.hasNext()) {
				Price p = iterator.next();
				int compareTo = p.getDay().compareTo(date);
				if (compareTo == 0) {
					iterator.remove();
					firePropertyChange("prices", p, null);
					return;
				} else if (compareTo > 0) {
					return;
				}
			}
		}
	}
}