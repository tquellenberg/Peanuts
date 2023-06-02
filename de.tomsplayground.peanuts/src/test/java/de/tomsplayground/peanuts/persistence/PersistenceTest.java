package de.tomsplayground.peanuts.persistence;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.process.Transaction;
import de.tomsplayground.peanuts.domain.process.Transfer;
import de.tomsplayground.peanuts.persistence.xstream.PersistenceService;
import de.tomsplayground.peanuts.util.Day;


public class PersistenceTest {

	private AccountManager accountManager;
	private Account account;
	private Account account2;

	@Before
	public void setup() {
		accountManager = new AccountManager();
	}

	private void createAccount() {
		account = accountManager.getOrCreateAccount("Test", Account.Type.BANK);
		account.putConfigurationValue("key123", "value456");
		account2 = accountManager.getOrCreateAccount("Test2", Account.Type.BANK);
	}

	private String write(IPersistenceService persistence, AccountManager accountManager) {
		StringWriter stringWriter = new StringWriter();
		persistence.write(accountManager, stringWriter);
		return stringWriter.toString();
	}
	
	@Test
	public void testEmptyAccountManager() throws Exception {
		IPersistenceService persistence = new PersistenceService();
		String xml = write(persistence, accountManager);
		System.out.println(xml);
		AccountManager accountManager2 = persistence.readAccountManager(xml);

		assertEquals(accountManager.getAccounts().size(), accountManager2.getAccounts().size());
	}

	@Test
	public void testAccountManager() throws Exception {
		account = accountManager.getOrCreateAccount("Test", Account.Type.BANK);
		account2 = accountManager.getOrCreateAccount("Test2", Account.Type.BANK);

		IPersistenceService persistence = new PersistenceService();
		String xml = write(persistence, accountManager);
		System.out.println(xml);
		AccountManager accountManager2 = persistence.readAccountManager(xml);

		assertEquals(accountManager.getAccounts().size(), accountManager2.getAccounts().size());
	}

	@Test
	public void testAccountManagerWithTtransactions() throws Exception {
		createAccount();
		account.addTransaction(new Transaction(Day.today(), new BigDecimal("12.34")));
		Transfer transfer = new Transfer(account, account2, new BigDecimal("1234.55"),
			Day.today());
		account.addTransaction(transfer.getTransferFrom());
		account2.addTransaction(transfer.getTransferTo());

		IPersistenceService persistence = new PersistenceService();
		String xml = write(persistence, accountManager);
		System.out.println(xml);
		AccountManager accountManager2 = persistence.readAccountManager(xml);

		assertEquals(accountManager.getAccounts().size(), accountManager2.getAccounts().size());
		Account accountToCheck = accountManager2.getAccounts().get(0);
		assertEquals(account.getName(), accountToCheck.getName());
		assertEquals(account.getBalance(Day.today()), accountToCheck.getBalance(Day.today()));
		assertEquals(account.getTransactions().size(), accountToCheck.getTransactions().size());
	}

	@Test
	public void testPersistence() throws Exception {
		createAccount();
		account.addTransaction(new Transaction(Day.today(), new BigDecimal("12.34")));
		Transfer transfer = new Transfer(account, account2, new BigDecimal("1234.55"),
			Day.today());
		account.addTransaction(transfer.getTransferFrom());
		account2.addTransaction(transfer.getTransferTo());

		IPersistenceService persistenceService = new PersistenceService();
		Persistence persistence = new Persistence();
		persistence.setPersistenceService(persistenceService);

		StringWriter stringWriter = new StringWriter();
		persistence.write(stringWriter, accountManager);
		System.out.println("XML:" + stringWriter);
		AccountManager accountManager2 = persistence.read(new StringReader(stringWriter.toString()));

		assertEquals(accountManager.getAccounts().size(), accountManager2.getAccounts().size());
		Account accountToCheck = accountManager2.getAccounts().get(0);
		assertEquals(account.getName(), accountToCheck.getName());
		assertEquals(account.getBalance(Day.today()), accountToCheck.getBalance(Day.today()));
		assertEquals(account.getTransactions().size(), accountToCheck.getTransactions().size());
	}

	@Test
	public void testReadExample() throws IOException {
		Reader in = new InputStreamReader(PersistenceTest.class.getResourceAsStream("/example.bpx"));
		String xml = IOUtils.toString(in);

		IPersistenceService persistenceService = new PersistenceService();
		Persistence persistence = new Persistence();
		persistence.setPersistenceService(persistenceService);

		AccountManager accountManager2 = persistence.read(new StringReader(xml));
		assertEquals(13, accountManager2.getAccounts().size());
	}

	@Test
	public void testUpdateConcurrentHashMap() throws IOException {
		Reader in = new InputStreamReader(PersistenceTest.class.getResourceAsStream("/ConcurrentHashMap.xml"));
		String xml = IOUtils.toString(in);

		String result = Persistence.updateConcurrentHashMap(xml);

		assertEquals("klasj fajsfl ajslf jaslfj aslkjals<displayConfiguration id=\"7420\"><entry><string>de.tomsplayground.peanuts.client.dashboard.SecurityWatchlist</string><string>1</string></entry><entry><string>chartType</string><string>one year</string></entry></displayConfiguration>skaj kajsfkaj lfkjaslkj", result);
	}

}
