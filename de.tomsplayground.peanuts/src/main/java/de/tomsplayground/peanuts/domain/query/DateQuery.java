package de.tomsplayground.peanuts.domain.query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.util.Day;

@XStreamAlias("dateQuery")
public class DateQuery implements IQuery {

	public enum TimeRange {
		ALL, THIS_YEAR, LAST_12_MONTH, MANUAL
	}

	private DateQuery.TimeRange timeRange;
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
		Calendar d = Calendar.getInstance();
		if (timeRange == TimeRange.THIS_YEAR) {
			d.set(Calendar.DAY_OF_MONTH, 1);
			d.set(Calendar.MONTH, Calendar.JANUARY);
			start = Day.fromCalendar(d);
			d.add(Calendar.YEAR, 1);
			end = Day.fromCalendar(d);
		} else if (timeRange == TimeRange.LAST_12_MONTH) {
			d.add(Calendar.DAY_OF_MONTH, 1);
			end = Day.fromCalendar(d);
			d.add(Calendar.YEAR, -1);
			start = Day.fromCalendar(d);
		}
	}

	private boolean isOkay(ITransaction t) {
		return (start == null || start.compareTo(t.getDay()) <= 0) &&
			(end == null || end.compareTo(t.getDay()) > 0);
	}

	@Override
	public List<ITransaction> filter(List<ITransaction> trans) {
		if (timeRange == TimeRange.ALL) {
			return trans;
		}
		calculateDates();
		List<ITransaction> result = new ArrayList<ITransaction>();
		for (ITransaction transaction : trans) {
			if (isOkay(transaction)) {
				result.add(transaction);
			}
		}
		return result;
	}

}
