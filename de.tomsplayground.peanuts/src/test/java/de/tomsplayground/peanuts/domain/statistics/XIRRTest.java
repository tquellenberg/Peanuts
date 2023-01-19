package de.tomsplayground.peanuts.domain.statistics;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Month;

import org.junit.Test;

import de.tomsplayground.peanuts.util.Day;

public class XIRRTest {

	@Test
	public void test0() {
		XIRR xirr = new XIRR();
		double entryVlaue = xirr.entryValue(new BigDecimal("206463"), 0, new BigDecimal("-0.238"));
		assertEquals(206463, entryVlaue, 0.0001);
		entryVlaue = xirr.entryValue(new BigDecimal("22500"), (double)104/365, new BigDecimal("-0.238").add(BigDecimal.ONE));
		assertEquals(24311.808487818595, entryVlaue, 0.0001);
		entryVlaue = xirr.entryValue(new BigDecimal("-175989"), (double)364/365, new BigDecimal("-0.238").add(BigDecimal.ONE));
		assertEquals(-230784.76777486206, entryVlaue, 0.0001);
	}

	@Test
	public void test01() {
		XIRR xirr = new XIRR();
		double entryVlaue = xirr.entryValue(new BigDecimal("-206463"), 0, new BigDecimal("-0.238"));
		assertEquals(-206463, entryVlaue, 0.0001);
		entryVlaue = xirr.entryValue(new BigDecimal("-22500"), (double)104/365, new BigDecimal("-0.238").add(BigDecimal.ONE));
		assertEquals(-24311.808487818595, entryVlaue, 0.0001);
		entryVlaue = xirr.entryValue(new BigDecimal("175989"), (double)364/365, new BigDecimal("-0.238").add(BigDecimal.ONE));
		assertEquals(230784.76777486206, entryVlaue, 0.0001);
	}

	@Test
	public void test1() {
		XIRR xirr = new XIRR();
		xirr.add(Day.firstDayOfYear(2014), new BigDecimal("-100"));
		xirr.add(Day.firstDayOfYear(2015), new BigDecimal("105"));

		BigDecimal rate = xirr.calculateValue();
		assertEquals(0, new BigDecimal("0.0500").compareTo(rate.round(new MathContext(3))));
	}

	@Test
	public void test1a() {
		XIRR xirr = new XIRR();
		xirr.add(Day.firstDayOfYear(2015), new BigDecimal("105"));
		xirr.add(Day.firstDayOfYear(2014), new BigDecimal("-100"));

		BigDecimal rate = xirr.calculateValue();
		assertEquals(0, new BigDecimal("0.0500").compareTo(rate.round(new MathContext(3))));
	}

	@Test
	public void test1b() {
		XIRR xirr = new XIRR();
		xirr.add(Day.firstDayOfYear(2014), new BigDecimal("100"));
		xirr.add(Day.firstDayOfYear(2015), new BigDecimal("-105"));
		xirr.add(Day.firstDayOfYear(2015), new BigDecimal("0"));

		BigDecimal rate = xirr.calculateValue();
		assertEquals(0, new BigDecimal("0.0500").compareTo(rate.round(new MathContext(3))));
	}

	@Test
	public void test10() {
		XIRR xirr = new XIRR();
		xirr.add(Day.firstDayOfYear(2013), new BigDecimal("0"));
		xirr.add(Day.firstDayOfYear(2014), new BigDecimal("-100"));
		xirr.add(Day.firstDayOfYear(2015), new BigDecimal("105"));

		BigDecimal rate = xirr.calculateValue();
		assertEquals(0, new BigDecimal("0.0500").compareTo(rate.round(new MathContext(3))));
	}

	@Test
	public void test11() {
		XIRR xirr = new XIRR();
		xirr.add(Day.firstDayOfYear(2013), new BigDecimal("0"));
		xirr.add(Day.firstDayOfYear(2014), new BigDecimal("-100"));
		xirr.add(Day.firstDayOfYear(2015), new BigDecimal("95"));

		BigDecimal rate = xirr.calculateValue();
		assertEquals(0, new BigDecimal("-0.0500").compareTo(rate.round(new MathContext(3))));
	}

	@Test
	public void test12() {
		XIRR xirr = new XIRR();
		xirr.add(Day.firstDayOfYear(2013), new BigDecimal("0"));
		xirr.add(Day.firstDayOfYear(2014), new BigDecimal("100"));
		xirr.add(Day.firstDayOfYear(2015), new BigDecimal("-95"));

		BigDecimal rate = xirr.calculateValue();
		assertEquals(0, new BigDecimal("-0.0500").compareTo(rate.round(new MathContext(3))));
	}

	@Test
	public void test2() {
		XIRR xirr = new XIRR();
		xirr.add(Day.of(2014, Month.MARCH, 24), new BigDecimal("-9490.61"));
		xirr.add(Day.of(2014, Month.SEPTEMBER, 19), new BigDecimal("14353.50"));

		BigDecimal rate = xirr.calculateValue();
		assertEquals(0, new BigDecimal("1.3246").compareTo(rate.round(new MathContext(5))));
	}

	@Test
	public void test3() {
		XIRR xirr = new XIRR();
		xirr.add(Day.of(2007, Month.JANUARY, 12), new BigDecimal("-10000"));
		xirr.add(Day.of(2008, Month.FEBRUARY, 14), new BigDecimal("2500"));
		xirr.add(Day.of(2008, Month.MARCH, 3), new BigDecimal("2000"));
		xirr.add(Day.of(2008, Month.JUNE, 14), new BigDecimal("3000"));
		xirr.add(Day.of(2008, Month.DECEMBER, 1), new BigDecimal("4000"));

		BigDecimal rate = xirr.calculateValue();
		assertEquals(0, new BigDecimal("0.10064").compareTo(rate.round(new MathContext(5))));
	}

	@Test
	public void test4() {
		XIRR xirr = new XIRR();
		xirr.add(Day.firstDayOfYear(2014), new BigDecimal("264040.870910"));
		xirr.add(Day.of(2014, Month.APRIL, 15), new BigDecimal("25500"));
		xirr.add(Day.lastDayOfYear(2014), new BigDecimal("-296753.935195"));

		BigDecimal rate = xirr.calculateValue();
		assertEquals(0, new BigDecimal("0.025632").compareTo(rate.round(new MathContext(5))));
	}

	@Test
	public void test5() {
		XIRR xirr = new XIRR();
		xirr.add(Day.firstDayOfYear(2011), new BigDecimal("-206463"));
		xirr.add(Day.of(2011, Month.APRIL, 15), new BigDecimal("-22500"));
		xirr.add(Day.lastDayOfYear(2011), new BigDecimal("175989"));

		BigDecimal rate = xirr.calculateValue();
		assertEquals(0, new BigDecimal("-0.23797").compareTo(rate.round(new MathContext(5))));
	}

	@Test
	public void test6() {
		XIRR xirr = new XIRR();
		xirr.add(Day.firstDayOfYear(2011), new BigDecimal("206463"));
		xirr.add(Day.of(2011, Month.APRIL, 15), new BigDecimal("22500"));
		xirr.add(Day.lastDayOfYear(2011), new BigDecimal("-175989"));

		BigDecimal rate = xirr.calculateValue();
		assertEquals(0, new BigDecimal("-0.23797").compareTo(rate.round(new MathContext(5))));
	}

	@Test
	public void extrem1() {
		XIRR xirr = new XIRR();
		xirr.add(Day.of(2021, Month.FEBRUARY, 1), new BigDecimal("-5308.77"));
		xirr.add(Day.of(2021, Month.FEBRUARY, 6), new BigDecimal("5749.27"));

		BigDecimal rate = xirr.calculateValue();
		assertEquals(0, new BigDecimal("335.64").compareTo(rate.round(new MathContext(5))));
	}

}
