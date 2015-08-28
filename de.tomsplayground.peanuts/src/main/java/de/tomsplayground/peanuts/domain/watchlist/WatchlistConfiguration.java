package de.tomsplayground.peanuts.domain.watchlist;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.base.Security;

@XStreamAlias("watchlist")
public class WatchlistConfiguration {

	public enum Type {
		MANUAL,
		MY_SECURITIES,
		ALL_SECURITIES
	}

	private String name;
	private Type type;
	private List<ISecuriityFilter> filters;
	private String sorting;

	public WatchlistConfiguration(String name) {
		this(name, Type.MANUAL, CategoryFilter.NO_FILTER, "");
	}

	public WatchlistConfiguration(String name, Type type, ISecuriityFilter filter, String sorting) {
		this.name = name;
		this.type = type;
		this.filters = Lists.newArrayList(filter);
		this.sorting = sorting;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Type getType() {
		return type;
	}
	public void setType(Type type) {
		this.type = type;
	}
	public ImmutableList<ISecuriityFilter> getFilters() {
		return ImmutableList.copyOf(filters);
	}
	public void setFilters(List<ISecuriityFilter> filters) {
		this.filters = filters;
	}
	public String getSorting() {
		return sorting;
	}
	public void setSorting(String sorting) {
		this.sorting = sorting;
	}

	public boolean accept(Security security) {
		for (ISecuriityFilter iSecuriityFilter : filters) {
			if (! iSecuriityFilter.accept(security)) {
				return false;
			}
		}
		return true;
	}
}
