package de.tomsplayground.peanuts.domain.statistics;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.domain.process.Price;

public class SimpleMovingAverage {

	private static final MathContext MC = new MathContext(10, RoundingMode.HALF_EVEN);

	private final int days;

	public SimpleMovingAverage(int days) {
		this.days = days;
	}

	public ImmutableList<IPrice> calculate(ImmutableList<? extends IPrice> prices) {
		BigDecimal d = new BigDecimal(days);
		List<IPrice> result = new ArrayList<IPrice>();
		Queue<BigDecimal> queue = new LinkedList<BigDecimal>();
		BigDecimal sum = BigDecimal.ZERO;
		for (IPrice price : prices) {
			BigDecimal value = price.getValue();
			queue.add(value);
			sum = sum.add(value);
			if (queue.size() == days) {
				BigDecimal average = sum.divide(d, MC);
				result.add(new Price(price.getDay(), average));
				sum = sum.subtract(queue.poll());
			}
		}
		return ImmutableList.copyOf(result);
	}

}
