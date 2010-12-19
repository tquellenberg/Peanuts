package de.tomsplayground.peanuts.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.Test;

import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Category;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.BankTransaction;
import de.tomsplayground.peanuts.domain.process.ITransferLocation;
import de.tomsplayground.util.Day;

public class AccountManagerTest {

	private static final String SECURITY_NAME = "Telekom AG";

	@Test
	public void testUnknownAccount() {
		AccountManager am = new AccountManager();

		ITransferLocation a1 = am.getOrCreateAccount("test", Account.Type.UNKNOWN);
		Account a2 = am.getOrCreateAccount("test", Account.Type.UNKNOWN);

		assertEquals(a1, a2);
		assertEquals(Account.Type.UNKNOWN, a2.getType());
	}

	@Test
	public void testUnknownAccount1() {
		AccountManager am = new AccountManager();

		ITransferLocation a1 = am.getOrCreateAccount("test", Account.Type.UNKNOWN);
		Account a2 = am.getOrCreateAccount("test", Account.Type.BANK);

		assertEquals(a1, a2);
		assertEquals(Account.Type.BANK, a2.getType());
	}

	@Test
	public void testUnknownAccount2() {
		AccountManager am = new AccountManager();

		ITransferLocation a1 = am.getOrCreateAccount("test", Account.Type.BANK);
		Account a2 = am.getOrCreateAccount("test", Account.Type.UNKNOWN);

		assertEquals(a1, a2);
		assertEquals(Account.Type.BANK, a2.getType());
	}

	@Test
	public void testResetAccount() {
		AccountManager am = new AccountManager();
		am.getOrCreateAccount("test", Account.Type.BANK);
		am.reset();

		assertEquals(0, am.getAccounts().size());
	}

	@Test
	public void testAddSecurities() {
		AccountManager securityManager = new AccountManager();
		securityManager.getOrCreateSecurity(SECURITY_NAME);

		assertEquals(1, securityManager.getSecurities().size());
		Security security = securityManager.getSecurities().iterator().next();
		assertEquals(SECURITY_NAME, security.getName());
	}

	@Test
	public void testResetSecurities() {
		AccountManager securityManager = new AccountManager();
		securityManager.getOrCreateSecurity(SECURITY_NAME);
		securityManager.reset();

		assertEquals(0, securityManager.getSecurities().size());
	}

	@Test
	public void testGetOrCreateCategory() {
		AccountManager cm = new AccountManager();
		Category cat = cm.getOrCreateCategory("test");

		assertEquals("test", cat.getName());
		assertEquals(Category.Type.UNKNOWN, cat.getType());

		Category cat2 = cm.getOrCreateCategory("test");
		assertEquals(1, cm.getCategories().size());
		assertEquals(cat, cat2);
	}

	@Test
	public void testAddCategory() {
		AccountManager cm = new AccountManager();
		cm.getOrCreateCategory("test");
		cm.addCategory(new Category("test", Category.Type.INCOME));

		assertEquals(1, cm.getCategories().size());
		assertEquals(Category.Type.INCOME, cm.getCategory("test").getType());
	}

	@Test
	public void testEqualCategory() {
		AccountManager cm = new AccountManager();
		Category cat1 = new Category("test", Category.Type.INCOME);
		Category cat2 = new Category("test", Category.Type.INCOME);
		cm.addCategory(cat1);
		cm.addCategory(cat2);

		assertEquals(1, cm.getCategories().size());
	}

	@Test
	public void testEqualCategory2() {
		AccountManager cm = new AccountManager();
		Category cat1 = new Category("test", Category.Type.UNKNOWN);
		Category cat2 = new Category("test", Category.Type.UNKNOWN);
		cm.addCategory(cat1);
		cm.addCategory(cat2);

		assertEquals(1, cm.getCategories().size());
	}

	@Test
	public void testEqualCategory3() {
		AccountManager cm = new AccountManager();
		Category cat1 = new Category("test", Category.Type.UNKNOWN);
		Category cat2 = new Category("test", Category.Type.INCOME);
		cm.addCategory(cat1);
		cm.addCategory(cat2);

		assertEquals(1, cm.getCategories().size());
	}

	@Test
	public void testEqualCategory4() {
		AccountManager cm = new AccountManager();
		Category cat1 = new Category("test", Category.Type.INCOME);
		Category cat2 = new Category("test", Category.Type.UNKNOWN);
		cm.addCategory(cat1);
		cm.addCategory(cat2);

		assertEquals(1, cm.getCategories().size());
	}

	@Test
	public void testNotEqualCategory() {
		AccountManager cm = new AccountManager();
		Category cat1 = new Category("test", Category.Type.INCOME);
		Category cat2 = new Category("test", Category.Type.EXPENSE);
		cm.addCategory(cat1);
		cm.addCategory(cat2);

		assertEquals(2, cm.getCategories().size());
	}

	@Test
	public void testCategoryChild() {
		AccountManager cm = new AccountManager();
		Category cat1 = new Category("test", Category.Type.INCOME);
		Category cat2 = new Category("test", Category.Type.INCOME);
		cat2.addChildCategory(new Category("child", Category.Type.INCOME));
		cm.addCategory(cat1);
		cm.addCategory(cat2);

		assertEquals(1, cm.getCategories().size());
		assertTrue(cm.getCategory("test").hasChildCategory("child"));
	}

	@Test
	public void testCategoryPath() {
		AccountManager cm = new AccountManager();
		Category cat = new Category("test", Category.Type.INCOME);
		Category cat2 = new Category("child", Category.Type.INCOME);
		cat.addChildCategory(cat2);
		cm.addCategory(cat);

		assertEquals("test : child", cat2.getPath());
		Category cat3 = cm.getCategoryByPath(cat2.getPath());
		assertTrue(cat3 == cat2);
		cat3 = cm.getCategoryByPath("test");
		assertTrue(cat3 == cat);
	}

	@Test
	public void testCategoryReset() {
		AccountManager cm = new AccountManager();
		cm.addCategory(new Category("test", Category.Type.INCOME));
		cm.reset();

		assertEquals(0, cm.getCategories().size());
	}

	@Test
	public void testCategoryByType() {
		AccountManager cm = new AccountManager();
		cm.addCategory(new Category("test1", Category.Type.INCOME));
		cm.addCategory(new Category("test2", Category.Type.EXPENSE));

		Set<Category> result = cm.getCategories(Category.Type.INCOME);
		assertEquals(1, result.size());
		assertEquals("test1", result.iterator().next().getName());
	}
	
	@Test
	public void testCategoryRemoveToLevel() throws Exception {
		AccountManager cm = new AccountManager();
		cm.addCategory(new Category("test1", Category.Type.INCOME));
		cm.addCategory(new Category("test2", Category.Type.EXPENSE));
		Account account = cm.getOrCreateAccount("account", Account.Type.BANK);
		BankTransaction transaction = new BankTransaction(new Day(), BigDecimal.ONE, "");
		transaction.setCategory(cm.getCategory("test2"));
		account.addTransaction(transaction);

		cm.removeCategory(cm.getCategory("test1"));
		Set<Category> result = cm.getCategories(Category.Type.INCOME);
		assertEquals(0, result.size());
		try {
			cm.removeCategory(cm.getCategory("test2"));
			fail("removeCategory must fail");
		} catch (IllegalStateException e) {
			// Okay
		}
	}

	@Test
	public void testCategoryRemove() throws Exception {
		AccountManager cm = new AccountManager();
		Category category = new Category("test1", Category.Type.INCOME);
		Category category2 = new Category("test2", Category.Type.INCOME);
		category.addChildCategory(category2);
		cm.addCategory(category);
		Account account = cm.getOrCreateAccount("account", Account.Type.BANK);
		BankTransaction transaction = new BankTransaction(new Day(), BigDecimal.ONE, "");
		transaction.setCategory(category2);
		account.addTransaction(transaction);

		cm.removeCategory(category2);
		Set<Category> result = cm.getCategories(Category.Type.INCOME);
		assertEquals(1, result.size());
		assertEquals(0, category.getChildCategories().size());
		assertEquals(category, transaction.getCategory());
	}
}
