package de.tomsplayground.peanuts.domain.process;

import static org.apache.commons.lang3.Validate.*;

import java.math.BigDecimal;

import de.tomsplayground.peanuts.util.Day;

public class PriceBuilder {

	private Day date = Day.today();
	private BigDecimal close = BigDecimal.ZERO;

	public void setDay(Day date) {
		notNull(date);
		this.date = date;
	}

	public void setClose(BigDecimal close) {
		notNull(close);
		this.close = close;
	}

	public Price build() {
		return new Price(date, close);
	}

}
