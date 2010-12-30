package de.tomsplayground.peanuts.domain.reporting.transaction;

import java.util.NoSuchElementException;

import junit.framework.Assert;

import org.junit.Test;

import de.tomsplayground.peanuts.domain.reporting.transaction.TimeIntervalReport.Interval;
import de.tomsplayground.util.Day;

public class DateIteratorTest {

	@Test
	public void testSimple() throws Exception {
		Day start = new Day(2010, 11, 1);
		Day end = new Day(2010, 11, 2);
		DateIterator dateIterator = new DateIterator(start, end, Interval.DAY);

		Assert.assertTrue(dateIterator.hasNext());
		Assert.assertEquals(start, dateIterator.next());
		Assert.assertTrue(dateIterator.hasNext());
		Assert.assertEquals(end, dateIterator.next());
		Assert.assertFalse(dateIterator.hasNext());
	}

	@Test
	public void currentRangeEnd() {
		Day start = new Day(2010, 11, 1);
		Day end = new Day(2011, 0, 1);
		DateIterator dateIterator = new DateIterator(start, end, Interval.MONTH);

		Assert.assertTrue(dateIterator.hasNext());
		Assert.assertEquals(start, dateIterator.next());
		Assert.assertEquals(end, dateIterator.currentRangeEnd());

		Assert.assertTrue(dateIterator.hasNext());
		Assert.assertEquals(end, dateIterator.next());
		Assert.assertEquals(new Day(2011, 1, 1), dateIterator.currentRangeEnd());

		Assert.assertFalse(dateIterator.hasNext());
	}

	@Test(expected = NoSuchElementException.class)
	public void exceptionOnNext() {
		Day start = new Day(2010, 11, 1);
		Day end = new Day(2010, 11, 1);
		DateIterator dateIterator = new DateIterator(start, end, Interval.MONTH);
		dateIterator.next();
		dateIterator.next();
	}

	@SuppressWarnings("unused")
	@Test(expected = IllegalArgumentException.class)
	public void exceptionOnWrongArguments() {
		Day start = new Day(2010, 11, 2);
		Day end = new Day(2010, 11, 1);
		new DateIterator(start, end, Interval.MONTH);
	}
}
