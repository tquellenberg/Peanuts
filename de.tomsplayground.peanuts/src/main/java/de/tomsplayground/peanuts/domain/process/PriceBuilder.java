package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;

import de.tomsplayground.util.Day;

public class PriceBuilder {

	private Day date = new Day();
	private BigDecimal open;
	private BigDecimal close;
	private BigDecimal high;
	private BigDecimal low;

	public void setDay(Day date) {
		this.date = date;
	}

	public void setOpen(BigDecimal open) {
		this.open = open;
	}

	public void setClose(BigDecimal close) {
		this.close = close;
	}

	public void setHigh(BigDecimal high) {
		this.high = high;
	}

	public void setLow(BigDecimal low) {
		this.low = low;
	}

	public Price build() {
		return new Price(date, open, close, high, low);
	}

}
