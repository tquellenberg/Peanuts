package de.tomsplayground.peanuts.domain.reporting.transaction;

import java.util.Iterator;
import java.util.NoSuchElementException;

import de.tomsplayground.peanuts.domain.reporting.transaction.TimeIntervalReport.Interval;
import de.tomsplayground.peanuts.util.Day;

public class DateIterator implements Iterator<Day> {

	private final Interval interval;
	private final Day end;

	private Day pointer;

	public DateIterator(Day start, Day end, Interval interval) {
		if (end.before(start)) {
			throw new IllegalArgumentException("end before start");
		}
		this.interval = interval;
		this.end = end;
		this.pointer = start;
	}

	@Override
	public boolean hasNext() {
		return pointer.beforeOrEquals(end);
	}

	@Override
	public Day next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		Day result = pointer;
		pointer = switch (interval) {
			case DAY -> pointer.addDays(1);
			case MONTH -> pointer.addMonth(1);
			case QUARTER -> pointer = pointer.addMonth(3);
			case YEAR -> pointer = pointer.addYear(1);
			case DECADE -> pointer = pointer.addYear(10);
		};
		return result;
	}

	public Day currentRangeEnd() {
		return pointer;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}