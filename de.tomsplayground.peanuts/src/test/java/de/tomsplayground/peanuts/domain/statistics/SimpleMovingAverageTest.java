package de.tomsplayground.peanuts.domain.statistics;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.time.Month;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.Helper;
import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.util.Day;


public class SimpleMovingAverageTest {

	@Test
	public void testMovingAverage() throws Exception {
		SimpleMovingAverage movingAverage = new SimpleMovingAverage(3);

		Day day = Day.of(2008, Month.OCTOBER, 18);
		ImmutableList<Price> prices = ImmutableList.of(
			new Price(day, new BigDecimal("5")),
			new Price(day.addDays(1), new BigDecimal("10")),
			new Price(day.addDays(2), new BigDecimal("15")),
			new Price(day.addDays(3), new BigDecimal("20")));
		List<IPrice> sma = movingAverage.calculate(prices);

		assertEquals(2, sma.size());
		IPrice price = sma.get(0);
		Helper.assertEquals(new BigDecimal("10"), price.getValue());
		assertEquals(Day.of(2008, Month.OCTOBER, 20), price.getDay());
		price = sma.get(1);
		Helper.assertEquals(new BigDecimal("15"), price.getValue());
		assertEquals(Day.of(2008, Month.OCTOBER, 21), price.getDay());
	}

}
