package de.tomsplayground.peanuts.domain.query;

import java.util.function.Predicate;

import de.tomsplayground.peanuts.domain.process.ITransaction;

public interface IQuery {

	Predicate<ITransaction> getPredicate();

}
