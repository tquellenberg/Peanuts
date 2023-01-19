package de.tomsplayground.peanuts.domain.watchlist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
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
	private final List<ISecuriityFilter> filters = new ArrayList<>();
	private String sorting;

	public WatchlistConfiguration(String name) {
		this(name, Type.MANUAL, Collections.emptyList(), "");
	}

	public WatchlistConfiguration(WatchlistConfiguration copy) {
		this(copy.name, copy.type, copy.filters, copy.sorting);
	}

	public WatchlistConfiguration(String name, Type type, List<ISecuriityFilter> filters, String sorting) {
		this.name = name;
		this.type = type;
		this.filters.addAll(filters);
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
		this.filters.clear();
		this.filters.addAll(filters);
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
		Stream<Security> result = switch (getType()) {
			case ALL_SECURITIES ->
				accountManager.getSecurities().parallelStream().filter(s -> ! s.isDeleted());
			case ALL_SECURITIES_INCL_DELETED ->
				accountManager.getSecurities().parallelStream();
			case MY_SECURITIES ->
				accountManager.getFullInventory().getSecurities().parallelStream();
			default ->
				Stream.empty();
		};
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
