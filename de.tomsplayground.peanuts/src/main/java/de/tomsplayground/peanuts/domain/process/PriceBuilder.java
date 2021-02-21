package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;

import de.tomsplayground.peanuts.util.Day;

public class PriceBuilder {

	private Day date = Day.today();
	private BigDecimal close;

	public void setDay(Day date) {
		this.date = date;
	}

	public void setClose(BigDecimal close) {
		this.close = close;
	}

	public Price build() {
		return new Price(date, close);
	}

}
