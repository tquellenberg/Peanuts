package de.tomsplayground.peanuts.client.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.swt.widgets.Display;

public abstract class UniqueAsyncExecution implements PropertyChangeListener {

	private volatile boolean executionInQueue;

	public abstract void doit(PropertyChangeEvent evt, Display display);

	public abstract Display getDisplay();

	@Override
	final public void propertyChange(final PropertyChangeEvent evt) {
		if (! executionInQueue) {
			executionInQueue = true;
			final Display display = getDisplay();
			display.asyncExec(() -> {
				executionInQueue = false;
				doit(evt, display);
			});
		}
	}

}
