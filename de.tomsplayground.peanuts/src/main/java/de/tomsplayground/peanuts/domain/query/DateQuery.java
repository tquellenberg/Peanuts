package de.tomsplayground.peanuts.domain.query;

import java.util.function.Predicate;

import com.google.common.base.Predicates;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.peanuts.util.Day;

@XStreamAlias("dateQuery")
public class DateQuery implements IQuery {

	public enum TimeRange {
		ALL, THIS_YEAR, LAST_12_MONTH, MANUAL
	}

	private final DateQuery.TimeRange timeRange;
	private Day start;
	private Day end;

	public DateQuery(DateQuery.TimeRange timeRange) {
		this.timeRange = timeRange;
	}

	/**
	 * Including c1.
	 * Excluding c2.
	 */
	public DateQuery(Day c1, Day c2) {
		if ( !c1.before(c2)) {
			throw new IllegalArgumentException("start not before end");
		}
		timeRange = DateQuery.TimeRange.MANUAL;
		start = c1;
		end = c2;
	}

	public DateQuery.TimeRange getTimeRange() {
		return timeRange;
	}

	public Day getStart() {
		return start;
	}

	public Day getEnd() {
		return end;
	}

	private void calculateDates() {
		Day now = Day.today();
		if (timeRange == TimeRange.THIS_YEAR) {
			start = Day.firstDayOfYear(now.year);
			end = start.addYear(1);
		} else if (timeRange == TimeRange.LAST_12_MONTH) {
			end = now.addDays(1);
			start = end.addYear(-1);
		}
	}

	private boolean isOkay(ITransaction t) {
		return (start == null || start.compareTo(t.getDay()) <= 0) &&
			(end == null || end.compareTo(t.getDay()) > 0);
	}

	@Override
	public Predicate<ITransaction> getPredicate() {
		if (timeRange == TimeRange.ALL) {
			return Predicates.alwaysTrue();
		}
		calculateDates();
		return new Predicate<ITransaction>() {
			@Override
			public boolean test(ITransaction input) {
				return isOkay(input);
			}
		};
	}

}
