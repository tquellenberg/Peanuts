package de.tomsplayground.peanuts.domain.statistics;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.OrderedMap;
import org.apache.commons.collections4.map.ListOrderedMap;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.config.ConfigurableSupport;
import de.tomsplayground.peanuts.config.IConfigurable;
import de.tomsplayground.peanuts.domain.base.INamedElement;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.base.InventoryEntry;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;

@XStreamAlias("securityCategoryMapping")
public class SecurityCategoryMapping extends ObservableModelObject implements INamedElement, IConfigurable {

	private String name;
	private final List<String> categories = new ArrayList<String>();
	private final Map<Security, String> mapping = new HashMap<Security, String>();
	private Map<String, String> displayConfiguration = new HashMap<String, String>();

	public SecurityCategoryMapping(String name, List<String> categories) {
		this.name = name;
		this.categories.addAll(categories);
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	public List<String> getCategories() {
		return new ArrayList<String>(categories);
	}

	public void setCategories(List<String> categories) {
		this.categories.clear();
		this.categories.addAll(categories);
	}

	public void renameCategory(String oldName, String newName) {
		for (Map.Entry<Security, String> entry : mapping.entrySet()) {
			if (oldName.equals(entry.getValue())) {
				entry.setValue(newName);
			}
		}
		categories.remove(oldName);
		categories.add(newName);
	}

	public void removeCategory(String category) {
		categories.remove(category);
		Set<Security> securities = getSecuritiesByCategory(category);
		for (Security security : securities) {
			setCategory(security, null);
		}
	}

	public void setCategory(Security security, String category) {
		if (category != null && ! categories.contains(category)) {
			categories.add(category);
		}
		mapping.put(security, category);
	}

	public void setSecuritiesForCategory(String category, Set<Security> securities) {
		if (category != null && ! categories.contains(category)) {
			categories.add(category);
		}
		for (Security security : getSecuritiesByCategory(category)) {
			mapping.remove(security);
		}
		for (Security security : securities) {
			mapping.put(security, category);
		}
	}

	public Set<Security> getAllSecurities() {
		return new HashSet<Security>(mapping.keySet());
	}

	public OrderedMap<String, BigDecimal> calculateCategoryValues(Inventory inventory) {
		OrderedMap<String, BigDecimal> values = new ListOrderedMap<String, BigDecimal>();
		for (String category : categories) {
			values.put(category, calculateCategoryValue(category, inventory));
		}
		return values;
	}

	private BigDecimal calculateCategoryValue(String category, Inventory inventory) {
		Set<Security> securities = getSecuritiesByCategory(category);
		if (securities == null || securities.isEmpty()) {
			return BigDecimal.ZERO;
		}
		BigDecimal result = BigDecimal.ZERO;
		Collection<InventoryEntry> entries = inventory.getEntries();
		for (Security security : securities) {
			for (InventoryEntry inventoryEntry : entries) {
				if (inventoryEntry.getSecurity().equals(security)) {
					result = result.add(inventoryEntry.getMarketValue(inventory.getDay()));
				}
			}
		}
		return result;
	}

	public Set<Security> getSecuritiesByCategory(String category) {
		Set<Security> securities = new HashSet<Security>();
		for (Map.Entry<Security, String> entry : mapping.entrySet()) {
			if (category.equals(entry.getValue())) {
				securities.add(entry.getKey());
			}
		}
		return securities;
	}

	public String getCategory(Security security) {
		return mapping.get(security);
	}

	private transient ConfigurableSupport configurableSupport;

	private ConfigurableSupport getConfigurableSupport() {
		if (displayConfiguration == null) {
			displayConfiguration = new HashMap<String, String>();
		}
		if (configurableSupport == null) {
			configurableSupport = new ConfigurableSupport(displayConfiguration, getPropertyChangeSupport());
		}
		return configurableSupport;
	}

	@Override
	public String getConfigurationValue(String key) {
		return getConfigurableSupport().getConfigurationValue(key);
	}

	@Override
	public void putConfigurationValue(String key, String value) {
		getConfigurableSupport().putConfigurationValue(key, value);
	}

}
