package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;

public interface ITrailingStrategy {

	/**
	 * Calculates the new stop value. 
	 * 
	 * @param stop Old stop value
	 * @param price Current stock price
	 * @return New stop value
	 */
	BigDecimal calculateStop(BigDecimal stop, Price price);

}
