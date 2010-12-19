package de.tomsplayground.peanuts.persistence;

import de.tomsplayground.peanuts.domain.base.AccountManager;

public interface IPersistenceService {

	String write(AccountManager accountManager);

	AccountManager readAccountManager(String xml);
}
