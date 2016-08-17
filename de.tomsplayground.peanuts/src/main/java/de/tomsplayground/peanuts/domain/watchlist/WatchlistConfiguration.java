package de.tomsplayground.peanuts.domain.watchlist;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Security;

@XStreamAlias("watchlist")
public class WatchlistConfiguration {

	public enum Type {
		MANUAL,
		MY_SECURITIES,
		ALL_SECURITIES,
		ALL_SECURITIES_INCL_DELETED
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

	public boolean isManuallyConfigured() {
		return type == Type.MANUAL;
	}

	public ImmutableList<Security> getSecuritiesByConfiguration(final AccountManager accountManager) {
		Stream<Security> result = null;
		switch (getType()) {
			case ALL_SECURITIES:
				result = accountManager.getSecurities().parallelStream().filter(s -> ! s.isDeleted());
				break;
			case ALL_SECURITIES_INCL_DELETED:
				result = accountManager.getSecurities().parallelStream();
				break;
			case MY_SECURITIES:
				result = accountManager.getFullInventory().getSecurities().parallelStream();
				break;
			default:
				result = Stream.empty();
		}
		return result.filter(s -> accept(s, accountManager))
			.collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
	}

	private boolean accept(Security security, AccountManager accountManager) {
		for (ISecuriityFilter iSecuriityFilter : filters) {
			if (! iSecuriityFilter.accept(security, accountManager)) {
				return false;
			}
		}
		return true;
	}
}
