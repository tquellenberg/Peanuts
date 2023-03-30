package de.tomsplayground.peanuts.persistence;

import java.io.Writer;

import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.process.IStockSplitProvider;

public interface IPersistenceService {

	void write(IStockSplitProvider accountManager, Writer writer);

	AccountManager readAccountManager(String xml);
}
