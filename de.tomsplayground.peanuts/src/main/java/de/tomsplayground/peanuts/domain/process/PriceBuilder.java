package de.tomsplayground.peanuts.domain.process;

import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;

import de.tomsplayground.peanuts.util.Day;

public class PriceBuilder {

	private Day date = Day.today();
	private BigDecimal close = BigDecimal.ZERO;

	public void setDay(Day date) {
		requireNonNull(date);
		this.date = date;
	}

	public void setClose(BigDecimal close) {
		requireNonNull(close);
		this.close = close;
	}

	public Price build() {
		return new Price(date, close);
	}

}
