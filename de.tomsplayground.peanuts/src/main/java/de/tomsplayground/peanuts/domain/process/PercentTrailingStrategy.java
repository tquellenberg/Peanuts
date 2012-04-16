package de.tomsplayground.peanuts.domain.process;

import static com.google.common.base.Preconditions.checkArgument;

import java.math.BigDecimal;

import javax.annotation.concurrent.Immutable;

@Immutable
public class PercentTrailingStrategy implements ITrailingStrategy {

	private final BigDecimal percent;
	
	public PercentTrailingStrategy(BigDecimal percent) {
		checkArgument(percent.signum() >= 0, "percent must be positive");
		checkArgument(BigDecimal.ONE.compareTo(percent) >= 0, "percent must be in the range 0..1");
		this.percent = percent;
	}
	
	@Override
	public BigDecimal calculateStop(BigDecimal stop, Price price) {
		BigDecimal newStop = price.getClose().multiply(BigDecimal.ONE.subtract(percent));
		if (newStop.compareTo(stop) > 0) {
			return newStop;
		}
		return stop;
	}

	public BigDecimal getPercent() {
		return percent;
	}
}
