package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class PriceProvider extends ObservableModelObject implements IPriceProvider {

	private final Security security;
	private ImmutableList<IPrice> prices = ImmutableList.of();

	public PriceProvider(Security security) {
		super();
		this.security = security;
	}

	@Override
	public Security getSecurity() {
		return security;
	}

	@Override
	public Currency getCurrency() {
		return security.getCurrency();
	}

	@Override
	public Day getMaxDate() {
		ImmutableList<IPrice> lp = prices;
		if (lp.isEmpty()) {
			return null;
		}
		return lp.get(lp.size() - 1).getDay();
	}

	@Override
	public Day getMinDate() {
		ImmutableList<IPrice> lp = prices;
		if (lp.isEmpty()) {
			return null;
		}
		return lp.get(0).getDay();
	}

	@Override
	public ImmutableList<IPrice> getPrices() {
		return prices;
	}

	@Override
	public ImmutableList<IPrice> getPrices(Day from, Day to) {
		if (from.after(to)) {
			throw new IllegalArgumentException("'from' after 'to'" + from + "->" + to);
		}
		int start = 0;
		int end = 0;
		ImmutableList<IPrice> lp = prices;
		for (IPrice p : lp) {
			if (p.getDay().compareTo(from) < 0) {
				start++;
			}
			if (p.getDay().compareTo(to) <= 0) {
				end++;
			}
		}
		return lp.subList(start, end);
	}

	@Override
	public IPrice getPrice(Day date) {
		ImmutableList<IPrice> lp = prices;
		if (lp.isEmpty()) {
			return new Price(date, BigDecimal.ZERO);
		}
		int pos = PeanutsUtil.binarySearch(lp, date);
		if (pos >= 0) {
			return lp.get(pos);
		} else {
			pos = -pos - 2;
			if (pos < 0) {
				return new Price(date, BigDecimal.ZERO);
			}
			return lp.get(pos);
		}
	}

	@Override
	public void setPrice(IPrice newPrice) {
		setPrice(newPrice, true);
	}

	@Override
	public void setPrice(IPrice newPrice, boolean overideExistingData) {
		IPrice oldPrice = setPriceInternal(newPrice, overideExistingData);
		if (oldPrice == null || !oldPrice.equals(newPrice)) {
			firePropertyChange("prices", oldPrice, newPrice);
		}
	}

	@Override
	public boolean setPrices(List<? extends IPrice> prices, boolean overideExistingData) {
		boolean change = false;
		if (this.prices.isEmpty()) {
			this.prices = ImmutableList.copyOf(prices.stream().sorted(new Comparator<IPrice>() {
				@Override
				public int compare(IPrice o1, IPrice o2) {
					return o1.getDay().compareTo(o2.getDay());
				}
			}).iterator());
			change = true;
		} else {
			for (IPrice price : prices) {
				IPrice oldValue = setPriceInternal(price, overideExistingData);
				change = change || oldValue == null || !oldValue.equals(price);
			}
		}
		if (change) {
			firePropertyChange("prices", null, prices);
		}
		return change;
	}

	private IPrice setPriceInternal(IPrice newPrice, boolean overideExistingData) {
		ImmutableList<IPrice> lp = prices;

		int binarySearch = PeanutsUtil.binarySearch(lp, newPrice.getDay());
		int s1, s2;
		IPrice oldPrice = null;
		if (binarySearch >= 0) {
			oldPrice = lp.get(binarySearch);
			if (overideExistingData) {
				// Update existing price with better values
				if (newPrice != Price.ZERO) {
					newPrice = new Price(oldPrice.getDay(), newPrice.getValue());
				}
			} else {
				// simulate no change
				return newPrice;
			}
			s1 = binarySearch;
			s2 = binarySearch + 1;
		} else {
			s1 = s2 = -binarySearch - 1;
		}

		Builder<IPrice> builder = ImmutableList.builderWithExpectedSize(prices.size() + 1);
		prices = builder
				.addAll(lp.subList(0, s1))
				.add(newPrice)
				.addAll(lp.subList(s2, lp.size()))
				.build();

		return oldPrice;
	}

	@Override
	public void removePrice(final Day date) {
		ImmutableList<IPrice> lp = prices;
		if (lp.isEmpty()) {
			return;
		}
		int pos = PeanutsUtil.binarySearch(lp, date);
		if (pos >= 0) {
			IPrice removed = lp.get(pos);
			
			Builder<IPrice> builder = ImmutableList.builder();
			prices = builder
				.addAll(lp.subList(0, pos))
				.addAll(lp.subList(pos+1, lp.size())).build();
			
			firePropertyChange("prices", removed, null);
		}
	}
}