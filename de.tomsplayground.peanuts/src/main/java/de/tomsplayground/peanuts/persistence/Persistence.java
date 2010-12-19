package de.tomsplayground.peanuts.persistence;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.CharBuffer;

import de.tomsplayground.peanuts.domain.base.AccountManager;

public class Persistence {

	IPersistenceService persistenceService;

	public void setPersistenceService(IPersistenceService persistence) {
		this.persistenceService = persistence;
	}

	public void write(Writer writer, AccountManager accountManager) {
		try {
			writer.write(persistenceService.write(accountManager));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public AccountManager read(Reader reader) {
		CharBuffer buffer = CharBuffer.allocate(1024);
		StringBuffer xml = new StringBuffer();
		try {
			while (reader.read(buffer) != -1) {
				buffer.flip();
				xml.append(buffer);
			}
			AccountManager readAccountManager = persistenceService.readAccountManager(xml.toString());
			readAccountManager.reconfigureAfterDeserialization();
			return readAccountManager;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
