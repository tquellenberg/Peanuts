package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;

import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.util.Day;
import junit.framework.TestCase;

public class InvestmentTransactionTest extends TestCase {

	private AccountManager acountManager;

	@Override
	public void setUp() {
		acountManager = new AccountManager();
	}

	public void testCloneContructor() {
		InvestmentTransaction trans = new InvestmentTransaction(Day.today(),
			acountManager.getOrCreateSecurity("test"), new BigDecimal("12.22"),
			new BigDecimal("10"), new BigDecimal("0.50"), InvestmentTransaction.Type.BUY);
		InvestmentTransaction trans2 = new InvestmentTransaction(trans);

		assertEquals(new BigDecimal("-122.70"), trans2.getAmount());
	}

	public void testBuy() {
		InvestmentTransaction trans = new InvestmentTransaction(Day.today(),
			acountManager.getOrCreateSecurity("test"), new BigDecimal("12.22"),
			new BigDecimal("10"), new BigDecimal("0.50"), InvestmentTransaction.Type.BUY);

		assertEquals(new BigDecimal("-122.70"), trans.getAmount());
	}

	public void testSell() {
		InvestmentTransaction trans = new InvestmentTransaction(Day.today(),
			acountManager.getOrCreateSecurity("test"), new BigDecimal("12.22"),
			new BigDecimal("10"), new BigDecimal("0.50"), InvestmentTransaction.Type.SELL);

		assertEquals(new BigDecimal("121.70"), trans.getAmount());
	}

	public void testExpense() {
		InvestmentTransaction trans = new InvestmentTransaction(Day.today(),
			acountManager.getOrCreateSecurity("test"), new BigDecimal("12.22"),
			new BigDecimal("10"), new BigDecimal("0.50"), InvestmentTransaction.Type.EXPENSE);

		assertEquals(new BigDecimal("-122.70"), trans.getAmount());
	}

	public void testIncome() {
		InvestmentTransaction trans = new InvestmentTransaction(Day.today(),
			acountManager.getOrCreateSecurity("test"), new BigDecimal("12.22"),
			new BigDecimal("10"), new BigDecimal("0.50"), InvestmentTransaction.Type.INCOME);

		assertEquals(new BigDecimal("121.70"), trans.getAmount());
	}

	public void testModifyAmount() {
		InvestmentTransaction trans = new InvestmentTransaction(Day.today(),
			acountManager.getOrCreateSecurity("test"), new BigDecimal("12.22"),
			new BigDecimal("10"), new BigDecimal("0.50"), InvestmentTransaction.Type.SELL);
		try {
			trans.setAmount(new BigDecimal("-130.00"));
			fail();
		} catch (UnsupportedOperationException e) {
			// okay
		}
	}

}
