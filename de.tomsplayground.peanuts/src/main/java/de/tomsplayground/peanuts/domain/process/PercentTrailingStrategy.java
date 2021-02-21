package de.tomsplayground.peanuts.domain.process;

import static com.google.common.base.Preconditions.*;

import java.math.BigDecimal;

public class PercentTrailingStrategy implements ITrailingStrategy {

	private final BigDecimal percent;

	public PercentTrailingStrategy(BigDecimal percent) {
		checkArgument(percent.signum() >= 0, "percent must be positive");
		checkArgument(BigDecimal.ONE.compareTo(percent) >= 0, "percent must be in the range 0..1");
		this.percent = percent;
	}

	@Override
	public BigDecimal calculateStop(BigDecimal stop, IPrice price) {
		BigDecimal newStop = price.getValue().multiply(BigDecimal.ONE.subtract(percent));
		if (newStop.compareTo(stop) > 0) {
			return newStop;
		}
		return stop;
	}

	public BigDecimal getPercent() {
		return percent;
	}
}
