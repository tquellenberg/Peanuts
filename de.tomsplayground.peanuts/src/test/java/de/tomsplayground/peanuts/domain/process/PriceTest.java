package de.tomsplayground.peanuts.domain.process;

import static org.junit.Assert.*;

import java.math.BigDecimal;

import org.junit.Test;

import de.tomsplayground.peanuts.util.Day;

public class PriceTest {

	private static final Day DATE = new Day(2008, 2, 29);
	private static final Day OTHER_DATE = new Day(2008, 2, 28);

	private static final BigDecimal TWO = new BigDecimal("2.00");
	private static final BigDecimal DIFFERENT_TWO = new BigDecimal("2.0");

	@Test
	public void testHashCode() {
		Price price1 = new Price(DATE, TWO);
		Price price2 = new Price(DATE, TWO);
		assertEquals(price1.hashCode(), price2.hashCode());

		Price price4 = new Price(OTHER_DATE, TWO);
		assertNotEquals(price1.hashCode(), price4.hashCode());

		// TODO: should be equals??
		Price price5 = new Price(DATE, DIFFERENT_TWO);
		assertNotEquals(price1.hashCode(), price5.hashCode());
	}

	@Test
	public void testEqualsObject() {
		Price price1 = new Price(DATE, TWO);
		Price price2 = new Price(DATE, TWO);
		assertEquals(price1, price2);

		Price price4 = new Price(OTHER_DATE, TWO);
		assertNotEquals(price1, price4);

		// TODO: should be equals??
		Price price5 = new Price(DATE, DIFFERENT_TWO);
		assertNotEquals(price5, price1);
	}

}
