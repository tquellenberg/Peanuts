package de.tomsplayground.peanuts.domain.reporting.transaction;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.process.BankTransaction;
import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.util.Day;


public class ReportTest {

	private AccountManager accountManager;
	private Account account1;

	@Before
	public void setup() {
		accountManager = new AccountManager();
		account1 = accountManager.getOrCreateAccount("test1", Account.Type.BANK);
		account1.addTransaction(new BankTransaction(Day.today(), new BigDecimal("10.00"), ""));
		Account account2 = accountManager.getOrCreateAccount("test2", Account.Type.BANK);
		account2.addTransaction(new BankTransaction(Day.today(), new BigDecimal("20.00"), ""));
	}

	@Test
	public void testPlain() {
		Report report = new Report("report1");

		assertTrue(report.allAccounts());
	}

	@Test
	public void testAccounts() {
		Report report = new Report("report1");
		report.setAccounts(accountManager.getAccounts());

		assertFalse(report.allAccounts());
		assertEquals(2, report.getAccounts().size());
		assertEquals(2, report.getTransactions().size());
	}

	@Test
	public void testSingleAccount() {
		Report report = new Report("report1");
		report.setAccounts(Collections.singleton(account1));

		assertEquals(1, report.getAccounts().size());
		List<ITransaction> transactions = report.getTransactions();
		assertEquals(1 , transactions.size());
		assertEquals(0, transactions.get(0).getAmount().compareTo(new BigDecimal("10.00")));
	}

	@Test
	public void testSplittedTransactions() {
		BankTransaction transaction = new BankTransaction(Day.today(), BigDecimal.ZERO, "Top");
		transaction.addSplit(new BankTransaction(Day.today(), BigDecimal.TEN, "split1"));
		transaction.addSplit(new BankTransaction(Day.today(), BigDecimal.TEN, "split1"));
		account1.addTransaction(transaction);

		Report report = new Report("report1");
		report.setAccounts(Collections.singleton(account1));
		List<ITransaction> transactions = report.getTransactions();
		assertEquals(3 , transactions.size());
	}
}
