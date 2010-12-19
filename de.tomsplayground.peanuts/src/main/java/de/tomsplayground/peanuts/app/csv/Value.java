package de.tomsplayground.peanuts.app.csv;


public class Value {

	public String str;

	public Value(String strv) {
		this.str = strv;
	}

	@Override
	public String toString() {
		if (str != null) {
			return str;
		}
		return "null";
	}

}
