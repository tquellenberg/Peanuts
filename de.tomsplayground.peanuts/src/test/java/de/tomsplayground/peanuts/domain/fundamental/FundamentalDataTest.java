package de.tomsplayground.peanuts.domain.fundamental;

import static org.junit.Assert.*;

import java.time.Month;

import org.junit.Test;

import de.tomsplayground.peanuts.util.Day;

public class FundamentalDataTest {

	@Test
	public void testStartEnd() {
		FundamentalData fundamentalData = new FundamentalData();
		fundamentalData.setYear(2017);
		assertEquals(Day.firstDayOfYear(2017), fundamentalData.getFiscalStartDay());
		assertEquals(Day.lastDayOfYear(2017), fundamentalData.getFiscalEndDay());
	}

	@Test
	public void testStartEndNov() {
		FundamentalData fundamentalData = new FundamentalData();
		fundamentalData.setYear(2017);
		fundamentalData.setFicalYearEndsMonth(-1);
		assertEquals(Day.of(2016, Month.DECEMBER, 1), fundamentalData.getFiscalStartDay());
		assertEquals(Day.of(2017, Month.NOVEMBER, 30), fundamentalData.getFiscalEndDay());
	}

	@Test
	public void testStartEndMar() {
		FundamentalData fundamentalData = new FundamentalData();
		fundamentalData.setYear(2017);
		fundamentalData.setFicalYearEndsMonth(-10);
		assertEquals(Day.of(2016, Month.MARCH, 1), fundamentalData.getFiscalStartDay());
		assertEquals(Day.of(2017, Month.FEBRUARY, 28), fundamentalData.getFiscalEndDay());
	}
}
