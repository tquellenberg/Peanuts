package de.tomsplayground.peanuts.util;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.time.Month;

import org.junit.Test;

public class DayTest {

	@Test
	public void testEquals() throws Exception {
		Day d1 = Day.of(2008, Month.JANUARY, 31);
		Day d2 = Day.of(2008, Month.JANUARY, 31);
		Day d3 = Day.of(2008, Month.JANUARY, 30);

		assertEquals(d1, d2);
		assertNotEquals(d3, d1);
	}

	@Test
	public void testHashCode() throws Exception {
		Day d1 = Day.of(2008, Month.FEBRUARY, 31);
		assertEquals(1028159, d1.hashCode());
	}

	@Test
	public void testFromLocalDate() throws Exception {
		LocalDate localDate = LocalDate.of(2008, Month.MARCH, 12);
		Day d = Day.from(localDate);

		assertEquals(Day.of(2008, Month.MARCH, 12), d);
	}

	@Test
	public void testCompare() throws Exception {
		Day d1 = Day.of(2008, Month.JANUARY, 30);
		Day d2 = Day.of(2008, Month.JANUARY, 31);

		assertEquals(0, d1.compareTo(d1));
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
		assertEquals(Day.of(2008, Month.MAY, 1), d);
		d = Day.fromString("2008-05-01");
		assertEquals(Day.of(2008, Month.MAY, 1), d);
	}

	@Test
	public void testToString() throws Exception {
		Day d = Day.of(2008, Month.MAY, 1);
		assertEquals("2008-5-1", d.toString());
	}

	@Test
	public void addDays() throws Exception {
		Day d = Day.of(2008, Month.FEBRUARY, 28);
		Day d2 = d.addDays(2);
		Day d3 = d.addDays(-28);

		assertEquals(Day.of(2008, Month.MARCH, 1), d2);
		assertEquals(Day.of(2008, Month.JANUARY, 31), d3);
	}

	@Test
	public void addYear() {
		Day d = Day.of(2008, Month.FEBRUARY, 28);
		Day d2 = d.addYear(1);

		assertEquals(Day.of(2009, Month.FEBRUARY, 28), d2);
	}

	@Test
	public void addMonth() throws Exception {
		Day d = Day.of(2008, Month.JANUARY, 31);
		Day d2 = d.addMonth(1);
		Day d3 = d.addMonth(-2);

		assertEquals(Day.of(2008, Month.FEBRUARY, 29), d2);
		assertEquals(Day.of(2007, Month.NOVEMBER, 30), d3);
	}

	@Test
	public void delta() {
		Day d1 = Day.of(2008, Month.JANUARY, 2);
		Day d2 = Day.of(2008, Month.JANUARY, 2);

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
		Day d1 = Day.of(2008, Month.JANUARY, 1);
		Day d2 = Day.of(2008, Month.JULY, 1);

		assertEquals(-180, d2.delta(d1));
	}
}
