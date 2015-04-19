package de.tomsplayground.peanuts.domain.base;

import com.google.common.collect.ImmutableList;

public interface ISecurityProvider {

	ImmutableList<Security> getSecurities();
}
