package de.tomsplayground.peanuts.domain.statistics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.statistics.Signal.Type;

public class SimpleMovingAverage {

	private final int days;
	private ImmutableList<Signal> signals = ImmutableList.of();

	public SimpleMovingAverage(int days) {
		this.days = days;
	}

	public ImmutableList<Price> calculate(ImmutableList<Price> prices) {
		List<Signal> s = new ArrayList<Signal>();
		s.clear();
		BigDecimal d = new BigDecimal(days);
		List<Price> result = new ArrayList<Price>();
		Queue<BigDecimal> queue = new LinkedList<BigDecimal>();
		BigDecimal sum = BigDecimal.ZERO;
		int compare = 0;
		for (Price price : prices) {
			BigDecimal value = price.getValue();
			queue.add(value);
			sum = sum.add(value);
			if (queue.size() == days) {
				BigDecimal average = sum.divide(d, RoundingMode.HALF_EVEN);
				result.add(new Price(price.getDay(), average));
				sum = sum.subtract(queue.poll());
				int newCompare = average.compareTo(value);
				if (newCompare != 0 && newCompare != compare) {
					if (compare != 0)
						s.add(new Signal(price.getDay(), newCompare < 0? Type.BUY:Type.SELL, price));
					compare = newCompare;
				}
			}
		}
		signals = ImmutableList.copyOf(s);
		return ImmutableList.copyOf(result);
	}
	
	public ImmutableList<Signal> getSignals() {
		return signals;
	}
}
