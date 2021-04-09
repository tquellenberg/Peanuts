package de.tomsplayground.peanuts.util;

import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tomsplayground.peanuts.events.Events;

public class Today {

	private final static Logger log = LoggerFactory.getLogger(Today.class);

	public static class DayChangeEvent {
		final Day today;

		public DayChangeEvent(Day newDay) {
			this.today = newDay;
		}
	}

	private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

	private Day today;

	public Today() {
		schedule();
		today = Day.fromCalendar(Calendar.getInstance());
	}

	private void schedule() {
		int offset = Math.min(60 - Calendar.getInstance().get(Calendar.MINUTE), 15);
		service.schedule(this::checkDay, offset, TimeUnit.MINUTES);
	}

	private void checkDay() {
		try {
			Day newday = Day.fromCalendar(Calendar.getInstance());
			if (! newday.equals(today)) {
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
		return today;
	}
}
