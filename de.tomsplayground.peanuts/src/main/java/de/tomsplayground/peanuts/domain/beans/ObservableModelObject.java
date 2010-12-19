package de.tomsplayground.peanuts.domain.beans;

import java.beans.PropertyChangeListener;

public class ObservableModelObject implements Cloneable {
	private transient PropertyChangeSupport propertyChangeSupport;

	protected PropertyChangeSupport getPropertyChangeSupport() {
		if (propertyChangeSupport == null) {
			propertyChangeSupport = new PropertyChangeSupport(this);
		}
		return propertyChangeSupport;
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		getPropertyChangeSupport().addPropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		getPropertyChangeSupport().addPropertyChangeListener(propertyName, listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		getPropertyChangeSupport().removePropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		getPropertyChangeSupport().removePropertyChangeListener(propertyName, listener);
	}

	protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
		getPropertyChangeSupport().firePropertyChange(propertyName, oldValue, newValue);
	}

	protected void firePropertyChange(String propertyName, int oldValue, int newValue) {
		getPropertyChangeSupport().firePropertyChange(propertyName, oldValue, newValue);
	}

	@Override
	protected Object clone() {
		ObservableModelObject o;
		try {
			o = (ObservableModelObject) super.clone();
			PropertyChangeListener[] listeners = getPropertyChangeSupport().getPropertyChangeListeners();
			o.propertyChangeSupport = new PropertyChangeSupport(o);
			for (PropertyChangeListener propertyChangeListener : listeners) {
				o.addPropertyChangeListener(propertyChangeListener);
			}
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
		return o;
	}
}
