package de.tomsplayground.peanuts.domain.statistics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.statistics.Signal.Type;

public class SimpleMovingAverage {

	private final int days;
	private final List<Signal> signals;

	public SimpleMovingAverage(int days) {
		this.days = days;
		this.signals = new ArrayList<Signal>();
	}

	public List<Price> calculate(List<Price> prices) {
		signals.clear();
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
						signals.add(new Signal(price.getDay(), newCompare < 0? Type.BUY:Type.SELL, price));
					compare = newCompare;
				}
			}
		}
		return result;
	}
	
	public List<Signal> getSignals() {
		return new ArrayList<Signal>(signals);
	}
}
