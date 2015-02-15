package de.tomsplayground.peanuts.scraping;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.text.ParseException;

import org.junit.Test;

import de.tomsplayground.util.Day;

public class ScrapingTest {

	@Test
	public void testDayWitText() throws ParseException {
		Day day = Scraping.scrapDay("12.12.2005Test");

		assertEquals(new Day(2005, 11, 12), day);
	}

	@Test
	public void testDecimal() {
		BigDecimal decimal = Scraping.scapBigDecimal("123.00");

		assertEquals(new BigDecimal("123.00"), decimal);
	}

	@Test
	public void testDecimalGerman() {
		BigDecimal decimal = Scraping.scapBigDecimal("123,00");

		assertEquals(new BigDecimal("123.00"), decimal);
	}

	@Test
	public void testDecimalLong() {
		BigDecimal decimal = Scraping.scapBigDecimal("1,234,567.00");

		assertEquals(new BigDecimal("1234567.00"), decimal);
	}

	@Test
	public void testDecimalGermanLong() {
		BigDecimal decimal = Scraping.scapBigDecimal("1.234.567,00");

		assertEquals(new BigDecimal("1234567.00"), decimal);
	}
}
