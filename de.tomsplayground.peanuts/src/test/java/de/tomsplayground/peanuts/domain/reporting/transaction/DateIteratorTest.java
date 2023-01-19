package de.tomsplayground.peanuts.domain.reporting.transaction;

import static org.junit.Assert.*;

import java.time.Month;
import java.util.NoSuchElementException;

import org.junit.Test;

import de.tomsplayground.peanuts.domain.reporting.transaction.TimeIntervalReport.Interval;
import de.tomsplayground.peanuts.util.Day;

public class DateIteratorTest {

	@Test
	public void testSimple() throws Exception {
		Day start = Day.of(2010, Month.DECEMBER, 1);
		Day end = Day.of(2010, Month.DECEMBER, 2);
		DateIterator dateIterator = new DateIterator(start, end, Interval.DAY);

		assertTrue(dateIterator.hasNext());
		assertEquals(start, dateIterator.next());
		assertTrue(dateIterator.hasNext());
		assertEquals(end, dateIterator.next());
		assertFalse(dateIterator.hasNext());
	}

	@Test
	public void currentRangeEnd() {
		Day start = Day.of(2010, Month.DECEMBER, 1);
		Day end = Day.firstDayOfYear(2011);
		DateIterator dateIterator = new DateIterator(start, end, Interval.MONTH);

		assertTrue(dateIterator.hasNext());
		assertEquals(start, dateIterator.next());
		assertEquals(end, dateIterator.currentRangeEnd());

		assertTrue(dateIterator.hasNext());
		assertEquals(end, dateIterator.next());
		assertEquals(Day.of(2011, Month.FEBRUARY, 1), dateIterator.currentRangeEnd());

		assertFalse(dateIterator.hasNext());
	}

	@Test(expected = NoSuchElementException.class)
	public void exceptionOnNext() {
		Day start = Day.of(2010, Month.DECEMBER, 1);
		Day end = Day.of(2010, Month.DECEMBER, 1);
		DateIterator dateIterator = new DateIterator(start, end, Interval.MONTH);
		dateIterator.next();
		dateIterator.next();
	}

	@Test(expected = IllegalArgumentException.class)
	public void exceptionOnWrongArguments() {
		Day start = Day.of(2010, Month.DECEMBER, 2);
		Day end = Day.of(2010, Month.DECEMBER, 1);
		new DateIterator(start, end, Interval.MONTH);
	}
}
