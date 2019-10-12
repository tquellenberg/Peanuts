package de.tomsplayground.peanuts.domain.query;

import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;

public class SecurityInvestmentQuery implements IQuery {

	private final Security security;

	public SecurityInvestmentQuery(Security security) {
		this.security = security;
	}

	private boolean isOkay(ITransaction input) {
		if (input instanceof InvestmentTransaction) {
			Security transSecurity = ((InvestmentTransaction) input).getSecurity();
			return transSecurity != null && StringUtils.equals(transSecurity.getISIN(), security.getISIN());
		}
		return false;
	}

	@Override
	public Predicate<ITransaction> getPredicate() {
		return this::isOkay;
	}

}
