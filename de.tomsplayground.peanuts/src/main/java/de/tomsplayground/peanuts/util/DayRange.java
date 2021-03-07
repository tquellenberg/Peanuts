package de.tomsplayground.peanuts.util;

import static org.apache.commons.lang3.Validate.*;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Date range with iterator, including start and end date.
 *
 */
public class DayRange implements Iterable<Day> {

	private final Day startDate;
	private final Day endDate;

	public DayRange(Day start, Day end) {
		isTrue(start.before(end) || start.equals(end));
		this.startDate = start;
		this.endDate = end;
	}

	private final class DayRangeIterator implements Iterator<Day> {

		private Day current;

		public DayRangeIterator() {
			current = startDate.addDays(-1);
		}

		@Override
		public boolean hasNext() {
			return current.before(endDate);
		}

		@Override
		public Day next() {
			if (! hasNext()) {
				throw new NoSuchElementException();
			}
			current = current.addDays(1);
			return current;
		}
	}

	@Override
	public Iterator<Day> iterator() {
		return new DayRangeIterator();
	}

	public Day getEndDate() {
		return endDate;
	}

	public Day getStartDate() {
		return startDate;
	}
}
