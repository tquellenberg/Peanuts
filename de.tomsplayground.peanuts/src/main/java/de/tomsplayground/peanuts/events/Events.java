package de.tomsplayground.peanuts.events;

import com.google.common.eventbus.EventBus;

public class Events {

	private static final EventBus eventBus = new EventBus("peanuts");

	public static EventBus getEventBus() {
		return eventBus;
	}
}
