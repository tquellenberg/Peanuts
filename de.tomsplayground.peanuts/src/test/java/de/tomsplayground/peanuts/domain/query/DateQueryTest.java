package de.tomsplayground.peanuts.domain.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import de.tomsplayground.util.Day;


public class DateQueryTest {

	@Test
	public void testTimeRange() {
		DateQuery report = new DateQuery(DateQuery.TimeRange.LAST_12_MONTH);

		assertEquals(DateQuery.TimeRange.LAST_12_MONTH, report.getTimeRange());
	}

	@Test
	public void testTimeRangeManual() {
		Day c1 = new Day(2001, 1, 1);
		Day c2 = new Day(2001, 5, 3);
		DateQuery report = new DateQuery(c1, c2);

		assertEquals(DateQuery.TimeRange.MANUAL, report.getTimeRange());
		assertEquals(c1, report.getStart());
		assertEquals(c2, report.getEnd());
	}

	@SuppressWarnings("unused")
	@Test
	public void testWrongTimeRangeManual() {
		Day c1 = new Day(2001, 1, 1);
		Day c2 = new Day(2001, 5, 3);

		try {
			new DateQuery(c2, c1);
			fail();
		} catch (IllegalArgumentException e) {
			// okay
		}
	}
}
