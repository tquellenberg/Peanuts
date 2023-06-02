package de.tomsplayground.peanuts.persistence;

import java.io.Writer;

import de.tomsplayground.peanuts.domain.base.AccountManager;

public interface IPersistenceService {

	void write(AccountManager accountManager, Writer writer);

	AccountManager readAccountManager(String xml);
}
