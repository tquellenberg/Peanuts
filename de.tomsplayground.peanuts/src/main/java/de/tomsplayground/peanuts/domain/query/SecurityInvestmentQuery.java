package de.tomsplayground.peanuts.domain.query;

import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;

public final class SecurityInvestmentQuery implements IQuery {

	private final Security security;

	public SecurityInvestmentQuery(Security security) {
		this.security = security;
	}

	@Override
	public Predicate<ITransaction> getPredicate() {
		return (t) -> {
			if (t instanceof InvestmentTransaction invT) {
				Security transSecurity = invT.getSecurity();
				return transSecurity != null && StringUtils.equals(transSecurity.getISIN(), security.getISIN());
			}
			return false;
		};
	}

}
