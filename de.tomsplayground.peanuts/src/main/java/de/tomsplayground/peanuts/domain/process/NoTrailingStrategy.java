package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;

import javax.annotation.concurrent.Immutable;

@Immutable
public class NoTrailingStrategy implements ITrailingStrategy {

	@Override
	public BigDecimal calculateStop(BigDecimal stop, Price price) {
		return stop;
	}

}
