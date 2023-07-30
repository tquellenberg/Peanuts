package de.tomsplayground.peanuts.domain.query;

import java.util.function.Predicate;

import de.tomsplayground.peanuts.domain.process.ITransaction;

sealed public interface IQuery permits CategoryQuery, DateQuery, InvestmentQuery, SecurityInvestmentQuery {

	Predicate<ITransaction> getPredicate();

}
