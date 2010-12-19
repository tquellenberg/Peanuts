package de.tomsplayground.peanuts.domain.statistics;

import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.util.Day;

public class Signal {

	private final Day day;
	private final Type type;
	private final Price price;

	public enum Type {
		BUY,
		SELL
	}
	
	public Signal(Day day, Type type, Price price) {
		this.day = day;
		this.type = type;
		this.price = price;
	}
	
	public Day getDay() {
		return day;
	}
	
	public Type getType() {
		return type;
	}
	
	public Price getPrice() {
		return price;
	}
}
