package de.tomsplayground.peanuts.domain.beans;

import java.beans.IndexedPropertyChangeEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class PropertyChangeSupport {

	private final Object source;

	private final Map<String, PropertyChangeSupport> children = new HashMap<String, PropertyChangeSupport>();

	private final Set<PropertyChangeListener> listeners = new CopyOnWriteArraySet<PropertyChangeListener>();

	public PropertyChangeSupport(Object sourceBean) {
		if (sourceBean == null) {
			throw new NullPointerException();
		}
		source = sourceBean;
	}

	/**
	 * Add a PropertyChangeListener to the listener list. The listener is
	 * registered for all properties. If
	 * <code>listener</code> is null, no exception is thrown and no action is
	 * taken.
	 *
	 * @param listener
	 *            The PropertyChangeListener to be added
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		if (listener == null) {
			return;
		}
		if (listener instanceof PropertyChangeListenerProxy) {
			PropertyChangeListenerProxy proxy = (PropertyChangeListenerProxy) listener;
			// Call two argument add method.
			addPropertyChangeListener(proxy.getPropertyName(), proxy.getListener());
		} else {
			listeners.add(listener);
		}
	}

	/**
	 * Remove a PropertyChangeListener from the listener list. This removes a
	 * PropertyChangeListener that was registered for all properties. If
	 * <code>listener</code> is null, or was never added, no exception is thrown
	 * and no action is taken.
	 *
	 * @param listener
	 *            The PropertyChangeListener to be removed
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		if (listener == null) {
			return;
		}
		if (listener instanceof PropertyChangeListenerProxy) {
			PropertyChangeListenerProxy proxy = (PropertyChangeListenerProxy) listener;
			// Call two argument remove method.
			removePropertyChangeListener(proxy.getPropertyName(), proxy.getListener());
		} else {
			listeners.remove(listener);
		}
	}

	/**
	 * Returns an array of all the listeners that were added to the
	 * PropertyChangeSupport object with addPropertyChangeListener().
	 * <p>
	 * If some listeners have been added with a named property, then the
	 * returned array will be a mixture of PropertyChangeListeners and
	 * <code>PropertyChangeListenerProxy</code>s. If the calling method is
	 * interested in distinguishing the listeners then it must test each element
	 * to see if it's a <code>PropertyChangeListenerProxy</code>, perform the
	 * cast, and examine the parameter.
	 *
	 * <pre>
	 * PropertyChangeListener[] listeners = bean.getPropertyChangeListeners();
	 * for (int i = 0; i &lt; listeners.length; i++) {
	 * 	if (listeners[i] instanceof PropertyChangeListenerProxy) {
	 * 		PropertyChangeListenerProxy proxy = (PropertyChangeListenerProxy) listeners[i];
	 * 		if (proxy.getPropertyName().equals(&quot;foo&quot;)) {
	 * 			// proxy is a PropertyChangeListener which was associated
	 * 			// with the property named &quot;foo&quot;
	 * 		}
	 * 	}
	 * }
	 *</pre>
	 *
	 * @see PropertyChangeListenerProxy
	 * @return all of the <code>PropertyChangeListeners</code> added or an empty
	 *         array if no listeners have been added
	 * @since 1.4
	 */
	public PropertyChangeListener[] getPropertyChangeListeners() {
		List<PropertyChangeListener> returnList = new ArrayList<PropertyChangeListener>();

		// Add all the PropertyChangeListeners
		returnList.addAll(listeners);

		// Add all the PropertyChangeListenerProxys
		synchronized (children) {
			Iterator<String> iterator = children.keySet().iterator();
			while (iterator.hasNext()) {
				String key = iterator.next();
				PropertyChangeSupport child = children.get(key);
				PropertyChangeListener[] childListeners = child.getPropertyChangeListeners();
				for (int index = childListeners.length - 1; index >= 0; index--) {
					returnList.add(new PropertyChangeListenerProxy(key, childListeners[index]));
				}
			}
		}
		return returnList.toArray(new PropertyChangeListener[0]);
	}

	/**
	 * Add a PropertyChangeListener for a specific property. The listener will
	 * be invoked only when a call on firePropertyChange names that specific
	 * property. For each property, the listener will be invoked. If
	 * <code>propertyName</code> or <code>listener</code>
	 * is null, no exception is thrown and no action is taken.
	 *
	 * @param propertyName
	 *            The name of the property to listen on.
	 * @param listener
	 *            The PropertyChangeListener to be added
	 */

	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		if (listener == null || propertyName == null) {
			return;
		}
		synchronized (children) {
			PropertyChangeSupport child = children.get(propertyName);
			if (child == null) {
				child = new PropertyChangeSupport(source);
				children.put(propertyName, child);
			}
			child.addPropertyChangeListener(listener);
		}
	}

	/**
	 * Remove a PropertyChangeListener for a specific property. If
	 * <code>propertyName</code> is null, no exception is thrown and
	 * no action is taken. If <code>listener</code> is null, or was never added
	 * for the specified property, no exception is thrown and no action is
	 * taken.
	 *
	 * @param propertyName
	 *            The name of the property that was listened on.
	 * @param listener
	 *            The PropertyChangeListener to be removed
	 */

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		if (listener == null || propertyName == null) {
			return;
		}
		synchronized (children) {
			PropertyChangeSupport child = children.get(propertyName);
			if (child == null) {
				return;
			}
			child.removePropertyChangeListener(listener);
		}
	}

	/**
	 * Returns an array of all the listeners which have been associated with the
	 * named property.
	 *
	 * @param propertyName
	 *            The name of the property being listened to
	 * @return all of the <code>PropertyChangeListeners</code> associated with
	 *         the named property. If no such listeners have been added, or if
	 *         <code>propertyName</code> is null, an empty array is returned.
	 */
	public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
		List<PropertyChangeListener> returnList = new ArrayList<PropertyChangeListener>();
		if (propertyName != null) {
			synchronized (children) {
				PropertyChangeSupport support = children.get(propertyName);
				if (support != null) {
					returnList.addAll(Arrays.asList(support.getPropertyChangeListeners()));
				}
			}
		}
		return returnList.toArray(new PropertyChangeListener[0]);
	}

	/**
	 * Report a bound property update to any registered listeners. No event is
	 * fired if old and new are equal and non-null.
	 *
	 * @param propertyName
	 *            The programmatic name of the property that was changed.
	 * @param oldValue
	 *            The old value of the property.
	 * @param newValue
	 *            The new value of the property.
	 */
	public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
		if (oldValue != null && newValue != null && oldValue.equals(newValue)) {
			return;
		}
		firePropertyChange(new PropertyChangeEvent(source, propertyName, oldValue, newValue));
	}

	/**
	 * Report an int bound property update to any registered listeners. No event
	 * is fired if old and new are equal and non-null.
	 * <p>
	 * This is merely a convenience wrapper around the more general
	 * firePropertyChange method that takes Object values.
	 *
	 * @param propertyName
	 *            The programmatic name of the property that was changed.
	 * @param oldValue
	 *            The old value of the property.
	 * @param newValue
	 *            The new value of the property.
	 */
	public void firePropertyChange(String propertyName, int oldValue, int newValue) {
		if (oldValue == newValue) {
			return;
		}
		firePropertyChange(propertyName, new Integer(oldValue), new Integer(newValue));
	}

	/**
	 * Report a boolean bound property update to any registered listeners. No
	 * event is fired if old and new are equal and non-null.
	 * <p>
	 * This is merely a convenience wrapper around the more general
	 * firePropertyChange method that takes Object values.
	 *
	 * @param propertyName
	 *            The programmatic name of the property that was changed.
	 * @param oldValue
	 *            The old value of the property.
	 * @param newValue
	 *            The new value of the property.
	 */
	public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
		if (oldValue == newValue) {
			return;
		}
		firePropertyChange(propertyName, Boolean.valueOf(oldValue), Boolean.valueOf(newValue));
	}

	/**
	 * Fire an existing PropertyChangeEvent to any registered listeners. No
	 * event is fired if the given event's old and new values are equal and
	 * non-null.
	 *
	 * @param evt
	 *            The PropertyChangeEvent object.
	 */
	public void firePropertyChange(PropertyChangeEvent evt) {
		Object oldValue = evt.getOldValue();
		Object newValue = evt.getNewValue();
		if (oldValue != null && newValue != null && oldValue.equals(newValue)) {
			return;
		}

		for (PropertyChangeListener target : listeners) {
			target.propertyChange(evt);
		}

		String propertyName = evt.getPropertyName();
		if (propertyName != null) {
			PropertyChangeSupport child;
			synchronized (children) {
				child = children.get(propertyName);
			}
			if (child != null) {
				child.firePropertyChange(evt);
			}
		}
	}

	/**
	 * Report a bound indexed property update to any registered listeners.
	 * <p>
	 * No event is fired if old and new values are equal and non-null.
	 *
	 * @param propertyName
	 *            The programmatic name of the property that was changed.
	 * @param index
	 *            index of the property element that was changed.
	 * @param oldValue
	 *            The old value of the property.
	 * @param newValue
	 *            The new value of the property.
	 * @since 1.5
	 */
	public void fireIndexedPropertyChange(String propertyName, int index, Object oldValue, Object newValue) {
		firePropertyChange(new IndexedPropertyChangeEvent(source, propertyName, oldValue, newValue, index));
	}

	/**
	 * Report an <code>int</code> bound indexed property update to any
	 * registered listeners.
	 * <p>
	 * No event is fired if old and new values are equal and non-null.
	 * <p>
	 * This is merely a convenience wrapper around the more general
	 * fireIndexedPropertyChange method which takes Object values.
	 *
	 * @param propertyName
	 *            The programmatic name of the property that was changed.
	 * @param index
	 *            index of the property element that was changed.
	 * @param oldValue
	 *            The old value of the property.
	 * @param newValue
	 *            The new value of the property.
	 * @since 1.5
	 */
	public void fireIndexedPropertyChange(String propertyName, int index, int oldValue, int newValue) {
		if (oldValue == newValue) {
			return;
		}
		fireIndexedPropertyChange(propertyName, index, new Integer(oldValue), new Integer(newValue));
	}

	/**
	 * Report a <code>boolean</code> bound indexed property update to any
	 * registered listeners.
	 * <p>
	 * No event is fired if old and new values are equal and non-null.
	 * <p>
	 * This is merely a convenience wrapper around the more general
	 * fireIndexedPropertyChange method which takes Object values.
	 *
	 * @param propertyName
	 *            The programmatic name of the property that was changed.
	 * @param index
	 *            index of the property element that was changed.
	 * @param oldValue
	 *            The old value of the property.
	 * @param newValue
	 *            The new value of the property.
	 * @since 1.5
	 */
	public void fireIndexedPropertyChange(String propertyName, int index, boolean oldValue, boolean newValue) {
		if (oldValue == newValue) {
			return;
		}
		fireIndexedPropertyChange(propertyName, index, Boolean.valueOf(oldValue), Boolean.valueOf(newValue));
	}

}
