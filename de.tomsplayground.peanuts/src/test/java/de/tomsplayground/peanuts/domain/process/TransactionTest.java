package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Category;
import de.tomsplayground.util.Day;

public class TransactionTest extends TestCase {

	private AccountManager acountManager;

	@Override
	public void setUp() {
		acountManager = new AccountManager();
	}

	public void testEmptySplit() {
		Transaction trans = new Transaction(new Day(), new BigDecimal("100.00"));

		trans.setSplits(null);

		assertEquals(0, trans.getSplits().size());
		assertEquals(new BigDecimal("100.00"), trans.getAmount());

		trans.setSplits(new ArrayList<Transaction>());

		assertEquals(0, trans.getSplits().size());
		assertEquals(new BigDecimal("100.00"), trans.getAmount());
	}

	public void testSplit() {
		Day now = new Day();
		Day notNow = new Day(1999, 1, 1);
		Transaction trans = new Transaction(now, new BigDecimal("0.00"));
		Category cat = acountManager.getOrCreateCategory("test");
		List<Transaction> splits = new ArrayList<Transaction>();
		splits.add(new Transaction(now, new BigDecimal("25.00"), cat, "memo"));
		splits.add(new Transaction(notNow, new BigDecimal("25.00"), cat, "memo"));
		trans.setSplits(splits);

		assertEquals(2, trans.getSplits().size());
		assertEquals(now, trans.getSplits().get(1).getDay());
		assertEquals(new BigDecimal("50.00"), trans.getAmount());
	}

	public void testUpdateAmount() {
		Day now = new Day();
		Transaction trans = new Transaction(now, new BigDecimal("100.00"));
		trans.setAmount(new BigDecimal("105.00"));

		assertEquals(new BigDecimal("105.00"), trans.getAmount());
	}

	public void testUpdateAmountWithSplit() {
		Day now = new Day();
		Transaction trans = new Transaction(now, new BigDecimal("100.00"));
		Category cat = acountManager.getOrCreateCategory("test");
		List<Transaction> splits = new ArrayList<Transaction>();
		splits.add(new Transaction(now, new BigDecimal("25.00"), cat, "memo"));
		splits.add(new Transaction(now, new BigDecimal("75.00"), cat, "memo"));
		trans.setSplits(splits);
		try {
			trans.setAmount(new BigDecimal("105.00"));
			fail();
		} catch (IllegalStateException e) {
			// okay
		}
		assertEquals(new BigDecimal("100.00"), trans.getAmount());
		assertEquals(2, trans.getSplits().size());
	}

	public void testUpdateDateWithSplits() {
		Day now = new Day();
		Day notNow = new Day(1999, 1, 1);
		Transaction trans = new Transaction(now, new BigDecimal("0.00"));
		Category cat = acountManager.getOrCreateCategory("test");
		List<Transaction> splits = new ArrayList<Transaction>();
		splits.add(new Transaction(now, new BigDecimal("25.00"), cat, "memo"));
		splits.add(new Transaction(now, new BigDecimal("25.00"), cat, "memo"));
		trans.setSplits(splits);

		trans.setDay(notNow);

		assertEquals(notNow, trans.getDay());
		assertEquals(notNow, trans.getSplits().get(0).getDay());
		assertEquals(notNow, trans.getSplits().get(1).getDay());
	}

	public void testUpdateSplitAmount() {
		Day now = new Day();
		Transaction trans = new Transaction(now, new BigDecimal("100.00"));
		Category cat = acountManager.getOrCreateCategory("test");
		List<Transaction> splits = new ArrayList<Transaction>();
		splits.add(new Transaction(now, new BigDecimal("25.00"), cat, "memo"));
		splits.add(new Transaction(now, new BigDecimal("75.00"), cat, "memo"));
		trans.setSplits(splits);
		splits.get(0).setAmount(new BigDecimal("35.00"));

		assertEquals(new BigDecimal("110.00"), trans.getAmount());
	}

	public void testClose() {
		Transaction trans = new Transaction(new Day(), new BigDecimal("100.00"));

		Transaction clone = (Transaction) trans.clone();
		assertNotNull(clone.splitChangeListener);
		assertTrue(trans.splitChangeListener != clone.splitChangeListener);
	}

}
