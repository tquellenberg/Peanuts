package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;

import de.tomsplayground.peanuts.util.PeanutsUtil;
import de.tomsplayground.util.Day;

public class AdjustedPrice implements IPrice {

	private final IPrice price;
	private final BigDecimal ratio;

	public AdjustedPrice(IPrice price, BigDecimal ratio) {
		this.price = price;
		this.ratio = ratio;
	}

	@Override
	public Day getDay() {
		return price.getDay();
	}

	@Override
	public BigDecimal getValue() {
		return price.getValue().multiply(ratio,PeanutsUtil. MC);
	}

}
