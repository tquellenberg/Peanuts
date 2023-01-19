package de.tomsplayground.peanuts.domain.process;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.time.Month;

import org.junit.Test;

import de.tomsplayground.peanuts.domain.process.SavedTransaction.Interval;
import de.tomsplayground.peanuts.util.Day;

public class SavedTransactionTest {

	private static final Transaction TRANSACTION = new Transaction(Day.of(2021, Month.OCTOBER, 3), new BigDecimal("100.34"));
	private static final Day START = Day.firstDayOfYear(2021);

	@Test
	public void testSimple() {
		SavedTransaction savedTransaction = new SavedTransaction("Test123", TRANSACTION);

		assertFalse(savedTransaction.isAutomaticExecution());
		assertNull(savedTransaction.getStart());
		assertEquals(SavedTransaction.Interval.MONTHLY, savedTransaction.getInterval());
	}

	@Test
	public void testSimple2() {
		SavedTransaction savedTransaction = new SavedTransaction("Test123", TRANSACTION, START, Interval.MONTHLY);

		assertTrue(savedTransaction.isAutomaticExecution());
		assertEquals(START, savedTransaction.getStart());
		assertEquals(START.addMonth(1), savedTransaction.nextExecution());
		assertEquals(SavedTransaction.Interval.MONTHLY, savedTransaction.getInterval());
	}

	@Test
	public void testExecution() {
		SavedTransaction savedTransaction = new SavedTransaction("Test123", TRANSACTION, START, Interval.MONTHLY);
		// One step
		savedTransaction.setStart(savedTransaction.nextExecution());

		assertEquals(START.addMonth(1), savedTransaction.getStart());
		assertEquals(START.addMonth(2), savedTransaction.nextExecution());
	}
}
