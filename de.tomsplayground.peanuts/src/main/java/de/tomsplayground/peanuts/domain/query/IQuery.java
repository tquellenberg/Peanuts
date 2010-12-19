package de.tomsplayground.peanuts.domain.query;

import java.util.List;

import de.tomsplayground.peanuts.domain.process.ITransaction;

public interface IQuery {

	List<ITransaction> filter(List<ITransaction> trans);

}
