package de.tomsplayground.peanuts.domain.statistics;

import java.math.BigDecimal;

import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.util.Day;

public class Signal {

	public static final Signal NO_SIGNAL = new Signal(Day.ZERO, Type.NONE, new Price(Day.ZERO, BigDecimal.ZERO));

	public final Day day;
	public final Type type;
	public final IPrice price;

	public enum Type {
		BUY,
		SELL,
		NONE
	}

	public Signal(Day day, Type type, IPrice price) {
		this.day = day;
		this.type = type;
		this.price = price;
	}

}
