package de.tomsplayground.peanuts.domain.reporting.transaction;

import static org.junit.Assert.*;

import java.util.NoSuchElementException;

import org.junit.Test;

import de.tomsplayground.peanuts.domain.reporting.transaction.TimeIntervalReport.Interval;
import de.tomsplayground.peanuts.util.Day;

public class DateIteratorTest {

	@Test
	public void testSimple() throws Exception {
		Day start = new Day(2010, 11, 1);
		Day end = new Day(2010, 11, 2);
		DateIterator dateIterator = new DateIterator(start, end, Interval.DAY);

		assertTrue(dateIterator.hasNext());
		assertEquals(start, dateIterator.next());
		assertTrue(dateIterator.hasNext());
		assertEquals(end, dateIterator.next());
		assertFalse(dateIterator.hasNext());
	}

	@Test
	public void currentRangeEnd() {
		Day start = new Day(2010, 11, 1);
		Day end = new Day(2011, 0, 1);
		DateIterator dateIterator = new DateIterator(start, end, Interval.MONTH);

		assertTrue(dateIterator.hasNext());
		assertEquals(start, dateIterator.next());
		assertEquals(end, dateIterator.currentRangeEnd());

		assertTrue(dateIterator.hasNext());
		assertEquals(end, dateIterator.next());
		assertEquals(new Day(2011, 1, 1), dateIterator.currentRangeEnd());

		assertFalse(dateIterator.hasNext());
	}

	@Test(expected = NoSuchElementException.class)
	public void exceptionOnNext() {
		Day start = new Day(2010, 11, 1);
		Day end = new Day(2010, 11, 1);
		DateIterator dateIterator = new DateIterator(start, end, Interval.MONTH);
		dateIterator.next();
		dateIterator.next();
	}

	@Test(expected = IllegalArgumentException.class)
	public void exceptionOnWrongArguments() {
		Day start = new Day(2010, 11, 2);
		Day end = new Day(2010, 11, 1);
		new DateIterator(start, end, Interval.MONTH);
	}
}
