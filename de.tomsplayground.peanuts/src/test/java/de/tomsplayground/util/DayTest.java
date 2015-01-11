package de.tomsplayground.util;

import static org.junit.Assert.*;

import java.util.Calendar;
import org.junit.Test;

import de.tomsplayground.util.Day;

public class DayTest {

	@Test
	public void testEquals() throws Exception {
		Day d1 = new Day(2008, 0, 31);
		Day d2 = new Day(2008, 0, 31);
		Day d3 = new Day(2008, 0, 30);

		assertEquals(d1, d2);
		assertFalse(d1.equals(d3));
	}

	@Test
	public void testHashCode() throws Exception {
		Day d1 = new Day(2008, 1, 31);
		assertTrue(1028159 == d1.hashCode());
	}

	@Test
	public void testFromCalendar() throws Exception {
		Calendar cal = Calendar.getInstance();
		cal.set(2008, 2, 12);
		Day d = Day.fromCalendar(cal);

		assertEquals(new Day(2008, 2, 12), d);
	}

	@Test
	public void testCompare() throws Exception {
		Day d1 = new Day(2008, 0, 30);
		Day d2 = new Day(2008, 0, 31);

		assertTrue(d1.compareTo(d1) == 0);
		assertTrue(d1.compareTo(d2) < 0);
		assertTrue(d2.compareTo(d1) > 0);
		assertTrue(d1.before(d2));
		assertTrue(d2.after(d1));
	}

	@Test
	public void testFromEmptyString() throws Exception {
		try {
			Day.fromString("");
			fail();
		} catch (IllegalArgumentException e) {
			// okay
		}
	}

	@Test
	public void testFromString() throws Exception {
		Day d = Day.fromString("2008-5-1");
		assertEquals(new Day(2008, 4, 1), d);
		d = Day.fromString("2008-05-01");
		assertEquals(new Day(2008, 4, 1), d);
	}

	@Test
	public void testToString() throws Exception {
		Day d = new Day(2008, 4, 1);
		assertEquals("2008-5-1", d.toString());
	}

	@Test
	public void addDays() throws Exception {
		Day d = new Day(2008, 1, 28);
		Day d2 = d.addDays(2);
		Day d3 = d.addDays(-28);

		assertEquals(new Day(2008, 2, 1), d2);
		assertEquals(new Day(2008, 0, 31), d3);
	}

	@Test
	public void addYear() {
		Day d = new Day(2008, 1, 28);
		Day d2 = d.addYear(1);

		assertEquals(new Day(2009, 1, 28), d2);
	}

	@Test
	public void addMonth() throws Exception {
		Day d = new Day(2008, 0, 31);
		Day d2 = d.addMonth(1);
		Day d3 = d.addMonth(-2);

		assertEquals(new Day(2008, 1, 29), d2);
		assertEquals(new Day(2007, 10, 30), d3);
	}

	@Test
	public void delta() {
		Day d1 = new Day(2008, 0, 2);
		Day d2 = new Day(2008, 0, 2);

		assertEquals(0, d1.delta(d2));
		d2 = d2.addDays(10);
		assertEquals(10, d1.delta(d2));
		d2 = d2.addMonth(2);
		assertEquals(70, d1.delta(d2));
		d2 = d2.addYear(-1);
		assertEquals(-290, d1.delta(d2));
	}

	@Test
	public void delta2() {
		Day d1 = new Day(2008, 0, 1);
		Day d2 = new Day(2008, 6, 1);

		assertEquals(-180, d2.delta(d1));
	}
}
