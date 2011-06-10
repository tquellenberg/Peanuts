package de.tomsplayground.peanuts.config;

import java.util.Map;

import de.tomsplayground.peanuts.domain.beans.PropertyChangeSupport;

public class ConfigurableSupport implements IConfigurable {

	private final Map<String, String> map;
	private final PropertyChangeSupport propertyChangeSupport;

	public ConfigurableSupport(Map<String, String> map, PropertyChangeSupport propertyChangeSupport) {
		this.map = map;
		this.propertyChangeSupport = propertyChangeSupport;
	}
	
	@Override
	public String getConfigurationValue(String key) {
		return map.get(key);
	}

	@Override
	public void putConfigurationValue(String key, String value) {
		String oldValue = map.get(key);
		map.put(key, value);
		if (propertyChangeSupport != null)
			propertyChangeSupport.firePropertyChange(key, oldValue, value);
	}
}
