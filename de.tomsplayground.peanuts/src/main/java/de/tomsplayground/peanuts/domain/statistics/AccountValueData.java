package de.tomsplayground.peanuts.domain.statistics;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import de.tomsplayground.util.Day;

public class AccountValueData {

	public static class Entry {
		private final Day day;
		private final BigDecimal value;
		private final BigDecimal investment;

		public Entry(Day day, BigDecimal value, BigDecimal investment) {
			this.day = day;
			this.value = value;
			this.investment = investment;
		}
		public Day getDay() {
			return day;
		}
		public BigDecimal getValue() {
			return value;
		}
		public BigDecimal getInvestment() {
			return investment;
		}
	}

	private final List<Entry> entries = new ArrayList<>();

	public void add(Day day, BigDecimal value, BigDecimal investment) {
		entries.add(new Entry(day, value, investment));
	}
	public List<Entry> getEntries() {
		return entries;
	}
	public Stream<Entry> stream() {
		return entries.stream();
	}
	public Stream<Entry> parallelStream() {
		return entries.parallelStream();
	}
}
