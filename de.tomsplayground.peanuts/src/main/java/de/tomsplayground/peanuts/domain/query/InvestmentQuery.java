package de.tomsplayground.peanuts.domain.query;

import java.util.function.Predicate;

import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;

public final class InvestmentQuery implements IQuery {

	@Override
	public Predicate<ITransaction> getPredicate() {
		return (t) -> (t instanceof InvestmentTransaction);
	}

}
