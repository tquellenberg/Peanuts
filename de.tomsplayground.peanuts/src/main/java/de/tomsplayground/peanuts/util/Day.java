package de.tomsplayground.peanuts.util;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class Day implements Serializable, Cloneable, Comparable<Day> {

	private static final long serialVersionUID = 817177201924284505L;

	public static final long SECONDS_PER_DAY = 24 * 60 * 60;

	public static final Day ZERO = new Day(0, 0, 1);
	public static final Day _1_1_1970 = new Day(1970, 0, 1);

	private static final DayCache DAY_CACHE = new DayCache();

	public final short day;	// 1..
	public final short month; // 0..
	public final short year;

	public static Day from(LocalDate date) {
		return of(date.getYear(), date.getMonthValue()-1, date.getDayOfMonth());
	}

	/**
	 *
	 * @param dayStr in Format yyyy-MM-dd
	 * @return
	 */
	public static Day fromString(String dayStr) {
		String[] split = dayStr.split("-");
		if (split.length != 3) {
			throw new IllegalArgumentException("Format must be yyyy-MM-dd: '"+dayStr+"'");
		}
		int year = Integer.parseInt(split[0]);
		int month = Integer.parseInt(split[1]);
		int day = Integer.parseInt(split[2]);
		return of(year, month-1, day);
	}

	public static Day today() {
		return DAY_CACHE.getToday();
	}

	Day(int year, int month, int day) {
		this.day = (short) day;
		this.month = (short) month;
		this.year = (short) year;
	}
	
	/**
	 * Create new Day object.
	 * @param year
	 * @param month 0..11
	 * @param day 1..31
	 */
	public static Day of(int year, int month, int day) {
		if (! (day > 0 && day <= 31)) {
			throw new IllegalArgumentException("day: 1-31");
		}
		if (! (month >= 0 && month <= 11)) {
			throw new IllegalArgumentException("month: 0-11");
		}
		return DAY_CACHE.getDay(year, month, day);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof Day) {
			Day d = (Day) obj;
			return day == d.day && month == d.month && year == d.year;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return day + (month << 5) + (year << 9);
	}

	@Override
	public String toString() {
		return year + "-" + (month+1) + "-" + day;
	}

	@Override
	public int compareTo(Day d) {
		if (year != d.year) {
			return year - d.year;
		}
		if (month != d.month) {
			return month - d.month;
		}
		return day - d.day;
	}

	public boolean before(Day d) {
		return this.compareTo(d) < 0;
	}

	public boolean beforeOrEquals(Day d) {
		return this.compareTo(d) <= 0;
	}

	public boolean after(Day d) {
		return this.compareTo(d) > 0;
	}

	public Day addDays(int amount) {
		return Day.from(toLocalDate().plus(amount, ChronoUnit.DAYS));
	}

	public Day addMonth(int amount) {
		return Day.from(toLocalDate().plus(amount, ChronoUnit.MONTHS));
	}

	public Day addYear(int amount) {
		return Day.from(toLocalDate().plus(amount, ChronoUnit.YEARS));
	}

	public YearMonth toYearMonth() {
		return YearMonth.of(year, month+1);
	}

	/**
	 * Calculates the difference in days between two dates
	 * on a base of 360 days per year.
	 */
	public int delta(Day d2) {
		int delta = (d2.year - year) * 360;
		delta = delta + ((d2.month - month) * 30);
		delta = delta + (d2.day - day);
		return delta;
	}

	public LocalDateTime toLocalDateTime() {
		return LocalDateTime.of(year, Month.of(month+1), day, 0, 0);
	}

	public LocalDate toLocalDate() {
		return LocalDate.of(year, Month.of(month+1), day);
	}

	public Date toDate() {
		return Date.from(toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	public Day adjustWorkday() {
		int i = toLocalDate().get(ChronoField.DAY_OF_WEEK);
		if (i == DayOfWeek.SUNDAY.getValue()) {
			return addDays(-2);
		}
		if (i == DayOfWeek.SATURDAY.getValue()) {
			return addDays(-1);
		}
		return this;
	}

}
