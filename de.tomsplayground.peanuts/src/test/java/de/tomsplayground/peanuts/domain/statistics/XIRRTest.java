package de.tomsplayground.peanuts.domain.statistics;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.MathContext;

import org.junit.Test;

import de.tomsplayground.util.Day;

public class XIRRTest {

	@Test
	public void test0() {
		XIRR xirr = new XIRR();
		double entryVlaue = xirr.entryVlaue(new BigDecimal("206463"), 0, new BigDecimal("-0.238"));
		assertEquals(206463, entryVlaue, 0.0001);
		entryVlaue = xirr.entryVlaue(new BigDecimal("22500"), 104, new BigDecimal("-0.238"));
		assertEquals(24311.808487818595, entryVlaue, 0.0001);
		entryVlaue = xirr.entryVlaue(new BigDecimal("-175989"), 364, new BigDecimal("-0.238"));
		assertEquals(-230784.76777486206, entryVlaue, 0.0001);
	}

	@Test
	public void test01() {
		XIRR xirr = new XIRR();
		double entryVlaue = xirr.entryVlaue(new BigDecimal("-206463"), 0, new BigDecimal("-0.238"));
		assertEquals(-206463, entryVlaue, 0.0001);
		entryVlaue = xirr.entryVlaue(new BigDecimal("-22500"), 104, new BigDecimal("-0.238"));
		assertEquals(-24311.808487818595, entryVlaue, 0.0001);
		entryVlaue = xirr.entryVlaue(new BigDecimal("175989"), 364, new BigDecimal("-0.238"));
		assertEquals(230784.76777486206, entryVlaue, 0.0001);
	}

	@Test
	public void test1() {
		XIRR xirr = new XIRR();	
		xirr.add(new Day(2014, 0, 1), new BigDecimal("-100"));
		xirr.add(new Day(2015, 0, 1), new BigDecimal("105"));
	
		BigDecimal rate = xirr.calculateValue();
		assertEquals(0, new BigDecimal("0.0500").compareTo(rate.round(new MathContext(3))));
	}

	@Test
	public void test1a() {
		XIRR xirr = new XIRR();	
		xirr.add(new Day(2015, 0, 1), new BigDecimal("105"));
		xirr.add(new Day(2014, 0, 1), new BigDecimal("-100"));
	
		BigDecimal rate = xirr.calculateValue();
		assertEquals(0, new BigDecimal("0.0500").compareTo(rate.round(new MathContext(3))));
	}

	@Test
	public void test1b() {
		XIRR xirr = new XIRR();	
		xirr.add(new Day(2014, 0, 1), new BigDecimal("100"));
		xirr.add(new Day(2015, 0, 1), new BigDecimal("-105"));
		xirr.add(new Day(2015, 0, 1), new BigDecimal("0"));
	
		BigDecimal rate = xirr.calculateValue();
		assertEquals(0, new BigDecimal("0.0500").compareTo(rate.round(new MathContext(3))));
	}

	@Test
	public void test10() {
		XIRR xirr = new XIRR();	
		xirr.add(new Day(2013, 0, 1), new BigDecimal("0"));
		xirr.add(new Day(2014, 0, 1), new BigDecimal("-100"));
		xirr.add(new Day(2015, 0, 1), new BigDecimal("105"));
	
		BigDecimal rate = xirr.calculateValue();
		assertEquals(0, new BigDecimal("0.0500").compareTo(rate.round(new MathContext(3))));
	}

	@Test
	public void test11() {
		XIRR xirr = new XIRR();	
		xirr.add(new Day(2013, 0, 1), new BigDecimal("0"));
		xirr.add(new Day(2014, 0, 1), new BigDecimal("-100"));
		xirr.add(new Day(2015, 0, 1), new BigDecimal("95"));
	
		BigDecimal rate = xirr.calculateValue();
		assertEquals(0, new BigDecimal("-0.0500").compareTo(rate.round(new MathContext(3))));
	}

	@Test
	public void test12() {
		XIRR xirr = new XIRR();	
		xirr.add(new Day(2013, 0, 1), new BigDecimal("0"));
		xirr.add(new Day(2014, 0, 1), new BigDecimal("100"));
		xirr.add(new Day(2015, 0, 1), new BigDecimal("-95"));
	
		BigDecimal rate = xirr.calculateValue();
		assertEquals(0, new BigDecimal("-0.0500").compareTo(rate.round(new MathContext(3))));
	}

	@Test
	public void test2() {
		XIRR xirr = new XIRR();	
		xirr.add(new Day(2014, 2, 24), new BigDecimal("-9490.61"));
		xirr.add(new Day(2014, 8, 19), new BigDecimal("14353.50"));
	
		BigDecimal rate = xirr.calculateValue();
		assertEquals(0, new BigDecimal("1.3246").compareTo(rate.round(new MathContext(5))));
	}
	
	@Test
	public void test3() {
		XIRR xirr = new XIRR();	
		xirr.add(new Day(2007, 0, 12), new BigDecimal("-10000"));
		xirr.add(new Day(2008, 1, 14), new BigDecimal("2500"));
		xirr.add(new Day(2008, 2, 3), new BigDecimal("2000"));
		xirr.add(new Day(2008, 5, 14), new BigDecimal("3000"));
		xirr.add(new Day(2008, 11, 1), new BigDecimal("4000"));
		
		BigDecimal rate = xirr.calculateValue();
		assertEquals(0, new BigDecimal("0.10064").compareTo(rate.round(new MathContext(5))));
	}
	
	@Test
	public void test4() {
		XIRR xirr = new XIRR();	
		xirr.add(new Day(2014, 0, 1), new BigDecimal("264040.870910"));
		xirr.add(new Day(2014, 3, 15), new BigDecimal("25500"));
		xirr.add(new Day(2014, 11, 31), new BigDecimal("-296753.935195"));
		
		BigDecimal rate = xirr.calculateValue();
		assertEquals(0, new BigDecimal("0.025632").compareTo(rate.round(new MathContext(5))));
	}

	@Test
	public void test5() {
		XIRR xirr = new XIRR();	
		xirr.add(new Day(2011, 0, 1), new BigDecimal("-206463"));
		xirr.add(new Day(2011, 3, 15), new BigDecimal("-22500"));
		xirr.add(new Day(2011, 11, 31), new BigDecimal("175989"));
		
		BigDecimal rate = xirr.calculateValue();
		assertEquals(0, new BigDecimal("-0.23797").compareTo(rate.round(new MathContext(5))));
	}
	
	@Test
	public void test6() {
		XIRR xirr = new XIRR();	
		xirr.add(new Day(2011, 0, 1), new BigDecimal("206463"));
		xirr.add(new Day(2011, 3, 15), new BigDecimal("22500"));
		xirr.add(new Day(2011, 11, 31), new BigDecimal("-175989"));
		
		BigDecimal rate = xirr.calculateValue();
		assertEquals(0, new BigDecimal("-0.23797").compareTo(rate.round(new MathContext(5))));
	}

}
