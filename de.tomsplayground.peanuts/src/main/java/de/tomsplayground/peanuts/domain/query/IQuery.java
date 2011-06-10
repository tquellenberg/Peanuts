package de.tomsplayground.peanuts.domain.query;

import com.google.common.base.Predicate;

import de.tomsplayground.peanuts.domain.process.ITransaction;

public interface IQuery {

	Predicate<ITransaction> getPredicate();

}
