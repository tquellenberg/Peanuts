package de.tomsplayground.peanuts.domain.watchlist;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.statistics.SecurityCategoryMapping;

@XStreamAlias("categoryfilter")
public class CategoryFilter implements ISecuriityFilter {

	public static final CategoryFilter NO_FILTER = new CategoryFilter("", Collections.<String>emptySet());

	private final String categoryName;
	private final Set<String> categoryValues = new HashSet<>();

	public CategoryFilter(String categoryName, Set<String> categoryValues) {
		this.categoryName = categoryName;
		this.categoryValues.addAll(categoryValues);
	}

	@Override
	public boolean accept(Security security, AccountManager accountManager) {
		SecurityCategoryMapping categoryMapping = accountManager.getSecurityCategoryMapping(categoryName);
		return categoryValues.contains(categoryMapping.getCategory(security));
	}

	public String getCategoryName() {
		return categoryName;
	}

	public ImmutableSet<String> getCategoryValues() {
		return ImmutableSet.copyOf(categoryValues);
	}
}
