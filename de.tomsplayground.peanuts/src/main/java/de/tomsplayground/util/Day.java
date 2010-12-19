package de.tomsplayground.util;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

/*
 * Unmodifiable day.
 */
public class Day implements Serializable, Cloneable, Comparable<Day>{

	private static final long serialVersionUID = 817177201924284505L;

	public final static Day ZERO = new Day(0, 0, 1);
	
	private final int day;
	private final int month;
	private final int year;

	public static Day fromCalendar(Calendar cal) {
		return new Day(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
	}
	
	public static Day fromDate(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return fromCalendar(cal);
	}
	
	/**
	 * 
	 * @param dayStr in Format yyyy-MM-dd
	 * @return
	 */
	public static Day fromString(String dayStr) {
		String[] split = dayStr.split("-");
		if (split.length != 3)
			throw new IllegalArgumentException("Format must be yyyy-MM-dd");
		int year = Integer.parseInt(split[0]);
		int month = Integer.parseInt(split[1]);
		int day = Integer.parseInt(split[2]);
		return new Day(year, month-1, day);
	}
	
	public Day() {
		Calendar cal = Calendar.getInstance();
		this.day = cal.get(Calendar.DAY_OF_MONTH);
		this.month = cal.get(Calendar.MONTH);
		this.year = cal.get(Calendar.YEAR);
	}
	
	public Day(int year, int month, int day) {
		if (! (day > 0 && day <= 31)) throw new IllegalArgumentException("day: 1-31");
		if (! (month >= 0 && month <= 11)) throw new IllegalArgumentException("month: 0-11");
		this.day = day;
		this.month = month;
		this.year = year;
	}

	public int getDay() {
		return day;
	}

	public int getMonth() {
		return month;
	}

	public int getYear() {
		return year;
	}

	public Calendar toCalendar() {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(year, month, day);
		return cal;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof Day) {
			Day d = (Day) obj;
			return year == d.year && month == d.month && day == d.day;
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
		if (year != d.year) return year - d.year;
		if (month != d.month) return month - d.month;
		return day - d.day;
	}

	public boolean before(Day d) {
		return this.compareTo(d) < 0;
	}

	public boolean after(Day d) {
		return this.compareTo(d) > 0;
	}

	public Day addDays(int amount) {
		Calendar cal = this.toCalendar();
		cal.add(Calendar.DAY_OF_MONTH, amount);
		return Day.fromCalendar(cal);
	}

	public Day addMonth(int amount) {
		Calendar cal = this.toCalendar();
		cal.add(Calendar.MONTH, amount);
		return Day.fromCalendar(cal);
	}

	public Day addYear(int amount) {
		Calendar cal = this.toCalendar();
		cal.add(Calendar.YEAR, amount);
		return Day.fromCalendar(cal);
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
	
	
}
