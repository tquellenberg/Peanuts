package de.tomsplayground.peanuts.util;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.Month;
import java.time.YearMonth;
import java.util.Currency;

import org.junit.Test;

import de.tomsplayground.peanuts.Helper;

public class PeanutsUtilTest {

	@Test
	public void testFormatPercent() {
		assertEquals("80,00%", PeanutsUtil.formatPercent(new BigDecimal("0.8")));
	}

	@Test
	public void testYearMonthFormat() {
		assertEquals("Feb. 2020", PeanutsUtil.formatMonth(YearMonth.of(2020, Month.FEBRUARY)));
	}

	@Test
	public void testDayFormat() {
		assertEquals("21.02.2020", PeanutsUtil.formatDate(Day.of(2020, Month.FEBRUARY, 21)));
	}

	@Test
	public void testparsePercent() throws ParseException {
		Helper.assertEquals(new BigDecimal("0.8"), PeanutsUtil.parsePercent("80,00%"));
	}

	@Test
	public void testFormatCurrencyRounding() {
		BigDecimal amount = new BigDecimal("105.525");
		String currency = PeanutsUtil.formatCurrency(amount, Currency.getInstance("USD"));
		assertEquals("105,53 $", currency);
	}

	@Test
	public void testFormatCurrencyMaximumFractionDigits() {
		BigDecimal amount = new BigDecimal("105.1234567890");
		String currency = PeanutsUtil.formatCurrency(amount, null);
		assertEquals("105,1235", currency);
	}

	@Test
	public void testFormatCurrencyMinimumFractionDigits() {
		BigDecimal amount = new BigDecimal("105");
		String currency = PeanutsUtil.formatCurrency(amount, null);
		assertEquals("105,00", currency);
	}

	@Test
	public void testFormatQuantity() {
		BigDecimal amount = new BigDecimal("105");
		String currency = PeanutsUtil.formatQuantity(amount);
		assertEquals("105", currency);
	}

	@Test
	public void testFormatQuantity2() {
		BigDecimal amount = new BigDecimal("105.1230");
		String currency = PeanutsUtil.formatQuantity(amount);
		assertEquals("105,123", currency);
	}

	@Test
	public void testFormatQuantity3() {
		BigDecimal amount = new BigDecimal("105.1234567890");
		String currency = PeanutsUtil.formatQuantity(amount);
		assertEquals("105,12345679", currency);
	}

}