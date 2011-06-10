package de.tomsplayground.peanuts.config;


public interface IConfigurable {

	String getConfigurationValue(String key);

	void putConfigurationValue(String key, String value);
}
