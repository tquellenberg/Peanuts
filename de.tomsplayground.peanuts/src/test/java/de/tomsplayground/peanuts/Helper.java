package de.tomsplayground.peanuts;

import java.math.BigDecimal;

public class Helper {

	public static void assertEquals(BigDecimal expected, BigDecimal actual) {
		if (expected.compareTo(actual)  == 0)
			return;
		throw new AssertionError("expected: <" + expected + "> but was: <" + actual + ">");
	}

}
