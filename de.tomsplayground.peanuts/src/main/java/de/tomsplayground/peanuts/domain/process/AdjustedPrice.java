package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import de.tomsplayground.util.Day;

public class AdjustedPrice implements IPrice {

	private final static MathContext MC = new MathContext(10, RoundingMode.HALF_EVEN);

	private final IPrice price;
	private final BigDecimal ratio;

	public AdjustedPrice(IPrice price, BigDecimal ratio) {
		this.price = price;
		this.ratio = ratio;
	}

	@Override
	public BigDecimal getClose() {
		return price.getClose().multiply(ratio, MC);
	}

	@Override
	public Day getDay() {
		return price.getDay();
	}

	@Override
	public BigDecimal getHigh() {
		return price.getHigh().multiply(ratio, MC);
	}

	@Override
	public BigDecimal getLow() {
		return price.getLow().multiply(ratio, MC);
	}

	@Override
	public BigDecimal getOpen() {
		return price.getOpen().multiply(ratio, MC);
	}

	@Override
	public BigDecimal getValue() {
		return price.getValue().multiply(ratio, MC);
	}

}
