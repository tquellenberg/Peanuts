package de.tomsplayground.peanuts.domain.fundamental;

import static org.junit.Assert.*;

import org.junit.Test;

import de.tomsplayground.peanuts.util.Day;

public class FundamentalDataTest {

	@Test
	public void testStartEnd() {
		FundamentalData fundamentalData = new FundamentalData();
		fundamentalData.setYear(2017);
		assertEquals(Day.of(2017, 0, 1), fundamentalData.getFiscalStartDay());
		assertEquals(Day.of(2017, 11, 31), fundamentalData.getFiscalEndDay());
	}

	@Test
	public void testStartEndNov() {
		FundamentalData fundamentalData = new FundamentalData();
		fundamentalData.setYear(2017);
		fundamentalData.setFicalYearEndsMonth(-1);
		assertEquals(Day.of(2016, 11, 1), fundamentalData.getFiscalStartDay());
		assertEquals(Day.of(2017, 10, 30), fundamentalData.getFiscalEndDay());
	}

	@Test
	public void testStartEndMar() {
		FundamentalData fundamentalData = new FundamentalData();
		fundamentalData.setYear(2017);
		fundamentalData.setFicalYearEndsMonth(-10);
		assertEquals(Day.of(2016, 2, 1), fundamentalData.getFiscalStartDay());
		assertEquals(Day.of(2017, 1, 28), fundamentalData.getFiscalEndDay());
	}
}
