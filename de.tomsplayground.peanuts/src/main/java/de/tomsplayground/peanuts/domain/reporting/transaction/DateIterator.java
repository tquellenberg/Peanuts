package de.tomsplayground.peanuts.domain.reporting.transaction;

import java.util.Calendar;
import java.util.Iterator;
import java.util.NoSuchElementException;

import de.tomsplayground.peanuts.domain.reporting.transaction.TimeIntervalReport.Interval;
import de.tomsplayground.peanuts.util.Day;

public class DateIterator implements Iterator<Day> {

	private final Calendar pointer;
	private final Interval interval;
	private final Day end;

	public DateIterator(Day start, Day end, Interval interval) {
		if (end.before(start)) {
			throw new IllegalArgumentException("end before start");
		}
		this.interval = interval;
		this.pointer = start.toCalendar();
		this.end = end;
	}

	@Override
	public boolean hasNext() {
		Day result = Day.fromCalendar(pointer);
		return !(result.after(end));
	}

	@Override
	public Day next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		Day result = Day.fromCalendar(pointer);
		switch (interval) {
			case DAY:
				pointer.add(Calendar.DAY_OF_MONTH, 1);
				break;
			case MONTH:
				pointer.add(Calendar.MONTH, 1);
				break;
			case QUARTER:
				pointer.add(Calendar.MONTH, 3);
				break;
			case YEAR:
				pointer.add(Calendar.YEAR, 1);
				break;
			case DECADE:
				pointer.add(Calendar.YEAR, 10);
				break;
		}
		return result;
	}

	public Day currentRangeEnd() {
		return Day.fromCalendar(pointer);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}