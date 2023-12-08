package de.tomsplayground.peanuts.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.tomsplayground.peanuts.events.Events;

public class DayCache {

	private final static int MONTH_SLOTS = 31;
	private final static int YEAR_SLOTS = MONTH_SLOTS * 12;
	private final static int YEAR = LocalDate.now().getYear();

	private final Day[] cache = new Day[YEAR_SLOTS * 30];

	private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(
			new ThreadFactoryBuilder().setNameFormat(DayCache.class.getSimpleName() + "-%d").setDaemon(true).build());

	private Day today;

	private final static Logger log = LoggerFactory.getLogger(DayCache.class);

	public static class DayChangeEvent {
		final Day today;

		public DayChangeEvent(Day newDay) {
			this.today = newDay;
		}
	}

	public DayCache() {
		schedule();
	}

	/**
	 * @param year
	 * @param month 0..11
	 * @param day   1..31
	 */
	private int indexOf(int year, Month month, int day) {
		int yearIndex = (YEAR - year) + 1;
		int index = (day - 1) + ((month.getValue()-1) * MONTH_SLOTS) + (yearIndex * YEAR_SLOTS);
		if (index < 0 || index >= cache.length) {
			log.info("Out of range: {}, {}, {}", year, month, day);
			return -1;
		}
		return index;
	}

	public Day getDay(int year, Month month, int day) {
		int index = indexOf(year, month, day);
		if (index >= 0) {
			Day d = cache[index];
			if (d == null) {
				d = new Day(year, month.getValue()-1, day);
				cache[index] = d;
			}
			return d;
		} else {
			return new Day(year, month.getValue()-1, day);
		}
	}

	private void schedule() {
		int offset = Math.min(60 - LocalDateTime.now().getMinute(), 15);
		service.schedule(this::checkDay, offset, TimeUnit.MINUTES);
	}

	private void checkDay() {
		try {
			Day newday = Day.from(LocalDate.now());
			if (!newday.equals(today)) {
				log.info("Day change to {}", newday);
				today = newday;
				sendEvent();
			} else {
				log.debug("No day change");
			}
		} finally {
			schedule();
		}
	}

	private void sendEvent() {
		Events.getEventBus().post(new DayChangeEvent(today));
	}

	public Day getToday() {
		if (today == null) {
			today = Day.from(LocalDate.now());
		}
		return today;
	}
}
