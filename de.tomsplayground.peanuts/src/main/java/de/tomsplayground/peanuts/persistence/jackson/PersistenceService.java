package de.tomsplayground.peanuts.persistence.jackson;

import java.io.Writer;

import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.persistence.IPersistenceService;

public class PersistenceService implements IPersistenceService {

	@Override
	public void write(AccountManager accountManager, Writer writer) {
	}

	@Override
	public AccountManager readAccountManager(String xml) {
		return null;
	}

}
