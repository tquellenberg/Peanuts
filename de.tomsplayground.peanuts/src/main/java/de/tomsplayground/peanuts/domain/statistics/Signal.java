package de.tomsplayground.peanuts.domain.statistics;

import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.util.Day;

public class Signal {

	public final Day day;
	public final Type type;
	public final IPrice price;

	public enum Type {
		BUY,
		SELL
	}
	
	public Signal(Day day, Type type, IPrice price) {
		this.day = day;
		this.type = type;
		this.price = price;
	}

}
