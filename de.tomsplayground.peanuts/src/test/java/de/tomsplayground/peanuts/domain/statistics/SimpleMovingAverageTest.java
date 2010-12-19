package de.tomsplayground.peanuts.domain.statistics;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.tomsplayground.peanuts.Helper;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.util.Day;


public class SimpleMovingAverageTest {

	@Test
	public void testMovingAverage() throws Exception {
		SimpleMovingAverage movingAverage = new SimpleMovingAverage(3);
		List<Price> prices = new ArrayList<Price>();
		
		Day day = new Day(2008, 9, 18);
		prices.add(new Price(day, new BigDecimal("5")));	day = day.addDays(1);
		prices.add(new Price(day, new BigDecimal("10")));	day = day.addDays(1);
		prices.add(new Price(day, new BigDecimal("15")));	day = day.addDays(1);
		prices.add(new Price(day, new BigDecimal("20")));	day = day.addDays(1);
		List<Price> sma = movingAverage.calculate(prices);
		
		assertEquals(2, sma.size());
		Price price = sma.get(0);
		Helper.assertEquals(new BigDecimal("10"), price.getValue());
		assertEquals(new Day(2008, 9, 20), price.getDay());
		price = sma.get(1);
		Helper.assertEquals(new BigDecimal("15"), price.getValue());
		assertEquals(new Day(2008, 9, 21), price.getDay());
	}
	
	@Test
	public void testNoSignals() throws Exception {
		SimpleMovingAverage movingAverage = new SimpleMovingAverage(3);
		List<Price> prices = new ArrayList<Price>();
		
		Day day = new Day(2008, 9, 18);
		prices.add(new Price(day, new BigDecimal("5")));	day = day.addDays(1);
		prices.add(new Price(day, new BigDecimal("10")));	day = day.addDays(1);
		prices.add(new Price(day, new BigDecimal("15")));	day = day.addDays(1);
		prices.add(new Price(day, new BigDecimal("20")));	day = day.addDays(1);
		movingAverage.calculate(prices);

		List<Signal> signals = movingAverage.getSignals();
		assertEquals(0, signals.size());
	}
	
	@Test
	public void testEqualValues() throws Exception {
		SimpleMovingAverage movingAverage = new SimpleMovingAverage(3);
		List<Price> prices = new ArrayList<Price>();
		
		Day day = new Day(2008, 9, 18);
		prices.add(new Price(day, new BigDecimal("5")));	day = day.addDays(1);
		prices.add(new Price(day, new BigDecimal("10")));	day = day.addDays(1);
		prices.add(new Price(day, new BigDecimal("14")));	day = day.addDays(1);
		// (Price == Avg) => no signal
		prices.add(new Price(day, new BigDecimal("12")));	day = day.addDays(1);
		movingAverage.calculate(prices);
		
		List<Signal> signals = movingAverage.getSignals();
		assertEquals(0, signals.size());
	}
}
