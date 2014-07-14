package de.tomsplayground.peanuts.domain.base;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@SuppressWarnings("serial")
@XStreamAlias("category")
public class Category implements Serializable, INamedElement {

	public enum Type {
		UNKNOWN, INCOME, EXPENSE
	}

	private String name;
	private Type type;
	final private Set<Category> children = new HashSet<Category>();
	private Category parent;

	public Category(String name, Type type) {
		this.name = name;
		this.type = type;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).
	       append("name", name).
	       append("type", type).
	       toString();
	}
	
	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Type getType() {
		return type;
	}

	public void addChildCategory(Category cat) {
		cat.parent = this;
		children.add(cat);
	}

	public Set<Category> getChildCategories() {
		return Collections.unmodifiableSet(children);
	}

	public boolean hasChildCategory(String childName) {
		return getCategory(childName) != null;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getPath() {
		String path = getName();
		if (parent != null) {
			path = parent.getPath() + " : " + path;
		}
		return path;
	}

	public Category getParent() {
		return parent;
	}

	public void setParent(Category parent) {
		this.parent = parent;
	}

	public Category getCategory(String childName) {
		for (Category cat : children) {
			if (cat.getName().equals(childName)) {
				return cat;
			}
		}
		return null;
	}

	public void removeChildCategory(Category categoryToRemove) {
		children.remove(categoryToRemove);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(71, 45).
			append(name).
			append(type).
			append(parent).
			toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		Category c = (Category) obj;
		return new EqualsBuilder().
			append(name, c.name).
			append(type, c.type).
			append(parent, c.parent).isEquals();
	}
	
}
