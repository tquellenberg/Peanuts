package de.tomsplayground.peanuts.domain.watchlist;

import de.tomsplayground.peanuts.domain.base.Security;

public interface ISecuriityFilter {

	boolean accept(Security security);
}
