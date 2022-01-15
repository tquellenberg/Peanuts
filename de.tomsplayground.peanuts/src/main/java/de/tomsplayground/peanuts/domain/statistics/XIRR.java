package de.tomsplayground.peanuts.domain.statistics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.google.common.collect.Lists;

import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class XIRR {

	private static final BigDecimal TWO = new BigDecimal("2");

	private static final BigDecimal minDistance = new BigDecimal("0.0000000001");

	private static class Entry implements Comparable<Entry> {
		final Day day;
		BigDecimal cashflow;

		// in percent of 360 days
		double dateDelta;

		public Entry(Day day, BigDecimal cashflow) {
			this.day = day;
			this.cashflow = cashflow;
		}
		public Day getDay() {
			return day;
		}
		public void addCashflow(BigDecimal cashflow) {
			this.cashflow = this.cashflow.add(cashflow);
		}
		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
		}
		@Override
		public int compareTo(Entry o) {
			return day.compareTo(o.day);
		}
	}

	private List<Entry> entries = Lists.newArrayList();

	private BigDecimal cachedValue = null;

	public void add(Day day, BigDecimal cashflow) {
		if (cashflow.signum() != 0) {
			entries.add(new Entry(day, cashflow));
			cachedValue = null;
		}
	}

	public BigDecimal calculateValue() {
		if (cachedValue == null) {
			cachedValue = calculateValueInternal();
		}
		return cachedValue;
	}

	private BigDecimal calculateValueInternal() {
		if (entries.isEmpty()) {
			return BigDecimal.ZERO;
		}
		prepareDates();
		checkNegative();

		BigDecimal irrGuess = new BigDecimal("0.9");
		BigDecimal rate = irrGuess;
		boolean wasHi = false;
		boolean wasLo = false;
		for (int i = 0; i < 500; i++) {
			double v = calculateValueForRate(rate);
			if (Math.abs(v) < 0.001) {
				break;
			}
			if (irrGuess.compareTo(minDistance) < 0) {
				break;
			}
			if (v > 0.0) {
				if (wasHi) {
					irrGuess = irrGuess.divide(TWO);
				}
				rate = rate.add(irrGuess);
				wasHi = false;
				wasLo = true;
			} else if (v < 0.0) {
				if (wasLo) {
					irrGuess = irrGuess.divide(TWO);
				}
				rate = rate.subtract(irrGuess);
				wasHi = true;
				wasLo = false;
			}
		}
		return rate;
	}

	private double calculateValueForRate(BigDecimal rate) {
		double v = 0;
		rate = rate.add(BigDecimal.ONE);
		for (Entry entry : entries) {
			v += entryValue(entry.cashflow, entry.dateDelta, rate);
		}
		return v;
	}

	protected double entryValue(BigDecimal cashflow, double dateDelta, BigDecimal rate) {
		return cashflow.doubleValue() / Math.pow(rate.doubleValue(), dateDelta);
	}

	private void prepareDates() {
		entries = entries.stream()
			.collect(Collectors.groupingBy(Entry::getDay,
				Collectors.reducing((a,b) -> {a.addCashflow(b.cashflow); return a;})))
			.values().stream()
			.map(Optional::get)
			.sorted()
			.collect(Collectors.toList());
		LocalDate minDate = entries.get(0).day.toLocalDate();
		BigDecimal yearLength = new BigDecimal(Year.of(minDate.getYear()).length());
		for (Entry entry : entries) {
			entry.dateDelta = new BigDecimal(ChronoUnit.DAYS.between(minDate, entry.day.toLocalDate())).divide(yearLength, PeanutsUtil.MC).doubleValue();
		}
	}

	private void checkNegative() {
		if (entries.get(entries.size()-1).cashflow.signum() == -1) {
			for (Entry entry : entries) {
				entry.cashflow = entry.cashflow.negate();
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		for (Entry entry : entries) {
			s.append(entry.toString()).append('\n');
		}
		return s.toString();
	}
}
