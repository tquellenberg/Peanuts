package de.tomsplayground.peanuts.domain;

import java.math.BigDecimal;
import java.util.List;

import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Category;
import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.peanuts.domain.process.Transaction;
import de.tomsplayground.peanuts.domain.process.Transfer;
import de.tomsplayground.peanuts.domain.process.TransferTransaction;
import de.tomsplayground.peanuts.util.Day;
import junit.framework.TestCase;

public class AccountTest extends TestCase {

	private static final String ACCOUNT_NAME1 = "name";
	private static final String ACCOUNT_NAME2 = "name2";
	private static final String ACCOUNT_NAME3 = "name3";

	Account account1;
	Account account2;
	Account unknownAccount;

	@Override
	public void setUp() {
		AccountManager accountManager = new AccountManager();
		account1 = accountManager.getOrCreateAccount(ACCOUNT_NAME1, Account.Type.BANK);
		account2 = accountManager.getOrCreateAccount(ACCOUNT_NAME2, Account.Type.BANK);
		unknownAccount = accountManager.getOrCreateAccount(ACCOUNT_NAME3, Account.Type.UNKNOWN);
	}

	public void testSimpleTransactions() {
		Transaction deposit = new Transaction(Day.today(), new BigDecimal("100.00"));
		account1.addTransaction(deposit);
		Transaction debit = new Transaction(Day.today(), new BigDecimal("-75.00"));
		account1.addTransaction(debit);

		assertEquals(2, account1.getTransactions().size());
		assertEquals(new BigDecimal("25.00"), account1.getBalance());
		assertEquals(new BigDecimal("100.00"), account1.getBalance(deposit));
		assertEquals(new BigDecimal("25.00"), account1.getBalance(debit));

		account1.reset();
		assertEquals(0, account1.getTransactions().size());
		assertEquals(BigDecimal.ZERO, account1.getBalance());
	}

	public void testSplitTransaction() {
		Transaction deposit = new Transaction(Day.today(), new BigDecimal("100.00"));
		Transaction split1 = new Transaction(Day.today(), new BigDecimal("30.00"));
		deposit.addSplit(split1);
		deposit.addSplit(new Transaction(Day.today(), new BigDecimal("70.00")));
		account1.addTransaction(deposit);
		Transaction debit = new Transaction(Day.today(), new BigDecimal("-75.00"));
		account1.addTransaction(debit);

		assertEquals(new BigDecimal("100.00"), account1.getBalance(deposit));
		assertEquals(new BigDecimal("30.00"), account1.getBalance(split1));
		assertEquals(0, account1.indexOf(split1));
		assertTrue(account1.isSplitTransaction(split1));
		assertEquals(deposit, account1.getParentTransaction(split1));
		assertFalse(account1.isSplitTransaction(deposit));
	}

	public void testTransfer() {
		Transfer t = new Transfer(account1, account2, new BigDecimal("75.00"),
			Day.today());
		account1.addTransaction(t.getTransferFrom());
		account2.addTransaction(t.getTransferTo());

		assertTrue(((TransferTransaction) t.getTransferFrom()).isSource());
		assertFalse(((TransferTransaction) t.getTransferTo()).isSource());
		assertEquals(new BigDecimal("-75.00"), account1.getBalance());
		assertEquals(new BigDecimal("75.00"), account2.getBalance());
	}

	public void testTransactionOrder() {
		Day d1 = Day.of(1999, 0, 1);
		Transaction t1 = new Transaction(d1, new BigDecimal("100.00"));
		account1.addTransaction(t1);
		Day d2 = Day.of(2000, 0, 1);
		Transaction t2 = new Transaction(d2, new BigDecimal("100.00"));
		account1.addTransaction(t2);

		List<ITransaction> transactions = account1.getTransactions();
		assertEquals(t1, transactions.get(0));
		assertEquals(t2, transactions.get(1));

		t2.setDay(Day.of(1998, 0, 1));
		transactions = account1.getTransactions();
		assertEquals(t2, transactions.get(0));
		assertEquals(t1, transactions.get(1));
	}

	public void testModifyDateForTransfer() {
		Day notNow = Day.of(1999, 0, 1);
		Transfer t = new Transfer(account1, account2, new BigDecimal("75.00"),
			Day.today());
		account1.addTransaction(t.getTransferFrom());
		account2.addTransaction(t.getTransferTo());
		((Transaction) account1.getTransactions().get(0)).setDay(notNow);

		assertEquals(notNow, account2.getTransactions().get(0).getDay());
	}

	public void testModifyTransfer() {
		Transfer t = new Transfer(account1, account2, new BigDecimal("75.00"), Day.today());
		t.setMemo("Memo");
		t.setLabel("Payee");
		t.setCategory(new Category("c1", Category.Type.EXPENSE));
		account1.addTransaction(t.getTransferFrom());
		account2.addTransaction(t.getTransferTo());
		Transaction t1 = (Transaction) account1.getTransactions().get(0);
		Transaction t2 = (Transaction) account2.getTransactions().get(0);

		t1.setAmount(new BigDecimal("-80.00"));
		t1.setMemo("memo2");
		Category c2 = new Category("c2", Category.Type.EXPENSE);
		t1.setCategory(c2);

		assertEquals(new BigDecimal("-80.00"), account1.getBalance());
		assertEquals(new BigDecimal("80.00"), account2.getBalance());
		assertEquals("memo2", t2.getMemo());
		assertEquals(c2, t2.getCategory());
	}

	public void testCloneTransfer() {
		Transfer t = new Transfer(account1, account2, new BigDecimal("75.00"), Day.today());
		t.setMemo("Memo");
		t.setLabel("Payee");
		t.setCategory(new Category("c1", Category.Type.EXPENSE));
		account1.addTransaction(t.getTransferFrom());
		account2.addTransaction(t.getTransferTo());
		Transaction t1 = (Transaction) account1.getTransactions().get(0);

		assertNotNull(t1.clone());
		account1.addTransaction(t1);

		assertEquals(2, account1.getTransactions().size());
		assertEquals(0, account1.getTransactions().get(0).getAmount().compareTo(new BigDecimal("-75.00")));
		assertEquals(0, account1.getTransactions().get(1).getAmount().compareTo(new BigDecimal("-75.00")));

		assertEquals(2, account2.getTransactions().size());
		assertEquals(0, account2.getTransactions().get(0).getAmount().compareTo(new BigDecimal("75.00")));
		assertEquals(0, account2.getTransactions().get(1).getAmount().compareTo(new BigDecimal("75.00")));
	}

	public void testCloneTransfer2() {
		Transfer t = new Transfer(account1, account2, new BigDecimal("75.00"), Day.today());
		t.setMemo("Memo");
		t.setLabel("Payee");
		t.setCategory(new Category("c1", Category.Type.EXPENSE));
		account1.addTransaction(t.getTransferFrom());
		account2.addTransaction(t.getTransferTo());
		Transaction t2 = (Transaction) account2.getTransactions().get(0);

		assertNotNull(t2.clone());
		account2.addTransaction(t2);

		assertEquals(2, account1.getTransactions().size());
		assertEquals(2, account2.getTransactions().size());
	}

	@SuppressWarnings("unused")
	public void testTransferEqualAccounts() {
		try {
			new Transfer(account1, account1, new BigDecimal("75.00"), Day.today());
			fail();
		} catch (IllegalStateException e) {
			// okay
		}
	}

	public void testRemoveNonExistionTransaction() {
		try {
			account1.removeTransaction(new Transaction(Day.today(), BigDecimal.ONE));
			fail();
		} catch (IllegalArgumentException e) {
			// okay
		}
	}

	public void testRemoveTransaction() {
		Transaction transaction1 = new Transaction(Day.today(), BigDecimal.ONE);
		Transaction transaction2 = new Transaction(Day.today(), BigDecimal.TEN);
		account1.addTransaction(transaction1);
		account1.addTransaction(transaction2);
		account1.removeTransaction(transaction1);

		assertEquals(1, account1.getTransactions().size());
		assertEquals(BigDecimal.TEN, account1.getBalance());
	}
	
	public void testRemoveTransferTransaction() {
		Transfer t = new Transfer(account1, account2, new BigDecimal("75.00"), Day.today());
		t.setMemo("Memo");
		t.setLabel("Payee");
		t.setCategory(new Category("c1", Category.Type.EXPENSE));
		account1.addTransaction(t.getTransferFrom());
		account2.addTransaction(t.getTransferTo());

		account1.removeTransaction(t.getTransferFrom());
		
		assertEquals(0, account2.getTransactions().size());
	}
	
	public void testChangeTransferTarget() {
		Transfer t = new Transfer(account1, account2, new BigDecimal("75.00"), Day.today());
		t.setMemo("Memo");
		t.setLabel("Payee");
		t.setCategory(new Category("c1", Category.Type.EXPENSE));
		account1.addTransaction(t.getTransferFrom());
		account2.addTransaction(t.getTransferTo());

		((TransferTransaction)t.getTransferFrom()).changeTarget(unknownAccount);
		
		assertEquals(0, account2.getTransactions().size());
		TransferTransaction tt1 = (TransferTransaction) account1.getTransactions().get(0);
		TransferTransaction tt2 = (TransferTransaction) unknownAccount.getTransactions().get(0);
		assertEquals(tt2, tt1.getComplement());
		assertEquals(tt1, tt2.getComplement());
		assertEquals(account1, tt2.getTarget());
		assertEquals(unknownAccount, tt1.getTarget());
	}

}
