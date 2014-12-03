package de.tomsplayground.peanuts.app.yahoo;

public class YahooSecurity {

	private final String symbol;
	private final String name;
	private final String exchange;

	public YahooSecurity(String symbol, String name, String exchange) {
		this.symbol = symbol;
		this.name = name;
		this.exchange = exchange;
	}

	public String getExchange() {
		return exchange;
	}
	public String getName() {
		return name;
	}
	public String getSymbol() {
		return symbol;
	}
}
