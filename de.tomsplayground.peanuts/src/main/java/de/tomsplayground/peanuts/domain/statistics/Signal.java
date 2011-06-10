package de.tomsplayground.peanuts.domain.statistics;

import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.util.Day;

public class Signal {

	public final Day day;
	public final Type type;
	public final Price price;

	public enum Type {
		BUY,
		SELL
	}
	
	public Signal(Day day, Type type, Price price) {
		this.day = day;
		this.type = type;
		this.price = price;
	}

}
