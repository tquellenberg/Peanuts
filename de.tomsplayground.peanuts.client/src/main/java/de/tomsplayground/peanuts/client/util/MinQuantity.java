package de.tomsplayground.peanuts.client.util;

import java.math.BigDecimal;

public class MinQuantity {

	public static final BigDecimal MIN_QANTITY = new BigDecimal("0.00000001");

	public static boolean isNotZero(BigDecimal quantity) {
		return quantity.abs().compareTo(MIN_QANTITY) > 0;
	}
}
