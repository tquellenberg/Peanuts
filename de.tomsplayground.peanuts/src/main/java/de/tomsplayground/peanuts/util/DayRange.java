package de.tomsplayground.peanuts.util;

import static org.apache.commons.lang3.Validate.*;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Date range with iterator, including start and end date.
 *
 */
public record DayRange(Day startDate, Day endDate) implements Iterable<Day> {

	public DayRange {
		isTrue(startDate.before(endDate) || startDate.equals(endDate));
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

}
