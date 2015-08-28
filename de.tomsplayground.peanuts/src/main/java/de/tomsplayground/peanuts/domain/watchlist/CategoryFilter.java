package de.tomsplayground.peanuts.domain.watchlist;

import java.util.Collections;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.base.Security;

@XStreamAlias("categoryfilter")
public class CategoryFilter implements ISecuriityFilter {

	public static final CategoryFilter NO_FILTER = new CategoryFilter("", Collections.<String>emptySet());

	private final String categoryName;
	private final Set<String> categoryValues;

	public CategoryFilter(String categoryName, Set<String> categoryValues) {
		this.categoryName = categoryName;
		this.categoryValues = categoryValues;
	}

	@Override
	public boolean accept(Security security) {
		return false;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public ImmutableSet<String> getCategoryValues() {
		return ImmutableSet.copyOf(categoryValues);
	}
}
