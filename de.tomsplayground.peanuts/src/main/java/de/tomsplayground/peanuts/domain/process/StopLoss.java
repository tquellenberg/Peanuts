package de.tomsplayground.peanuts.domain.process;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.util.Day;

@Immutable
@XStreamAlias("stoploss")
public class StopLoss {

	private final Security security;
	private final Day start;
	private final BigDecimal startPrice;
	private final ITrailingStrategy strategy;

	public StopLoss(Security security, Day start, BigDecimal startPrice, ITrailingStrategy strategy) {
		checkNotNull(security);
		checkNotNull(start);
		checkNotNull(startPrice);
		checkNotNull(strategy);
		this.security = security;
		this.start = start;
		this.startPrice = startPrice;
		this.strategy = strategy;
	}

	public ImmutableList<Price> getPrices(IPriceProvider priceProvider) {
		Builder<Price> builder = ImmutableList.builder();
		Day d = start;
		BigDecimal stop = startPrice;
		Day today = new Day();
		while (! d.after(today)) {
			Price price = priceProvider.getPrice(d);
			stop = strategy.calculateStop(stop, price);
			builder.add(new Price(d, stop));
			d = d.addDays(1);
		}
		return builder.build();
	}
	
	public Security getSecurity() {
		return security;
	}
	
	public Day getStart() {
		return start;
	}
	
	public BigDecimal getStartPrice() {
		return startPrice;
	}
	
	public ITrailingStrategy getStrategy() {
		return strategy;
	}
	
}
