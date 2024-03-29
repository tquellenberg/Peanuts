package de.tomsplayground.peanuts.domain.base;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.time.Month;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.process.BankTransaction;
import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.peanuts.util.Day;


public class TransactionProviderUtilTest {

	ImmutableList<ITransaction> trList = ImmutableList.<ITransaction>of(
		new BankTransaction(Day.of(2008, Month.JANUARY, 2), BigDecimal.ZERO, ""),
		new BankTransaction(Day.of(2008, Month.JANUARY, 4), BigDecimal.ZERO, ""),
		new BankTransaction(Day.of(2008, Month.JANUARY, 4), BigDecimal.ZERO, ""),
		new BankTransaction(Day.of(2008, Month.JANUARY, 6), BigDecimal.ZERO, ""),
		new BankTransaction(Day.of(2008, Month.JANUARY, 6), BigDecimal.ZERO, ""),
		new BankTransaction(Day.of(2008, Month.JANUARY, 26), BigDecimal.ZERO, ""),
		new BankTransaction(Day.of(2008, Month.JANUARY, 26), BigDecimal.ZERO, ""));

	@Test
	public void emptylist() throws Exception {
		Day from = Day.of(2008, Month.JANUARY, 2);
		Day to = Day.of(2008, Month.JANUARY, 12);
		List<ITransaction> byDate = TransactionProviderUtil.getTransactionsByDate(ImmutableList.of(), from, to);
		assertTrue(byDate.isEmpty());
	}

	@Test
	public void allNull() throws Exception {
		List<ITransaction> byDate = TransactionProviderUtil.getTransactionsByDate(trList, null, null);
		assertEquals(trList.size(), byDate.size());
	}

	@Test
	public void oneDay() {
		Day from = Day.of(2008, Month.JANUARY, 4);
		Day to = from;
		List<ITransaction> byDate = TransactionProviderUtil.getTransactionsByDate(trList, from, to);
		assertEquals(2, byDate.size());
		assertEquals(from, byDate.get(0).getDay());
		assertEquals(from, byDate.get(1).getDay());
	}

	@Test
	public void notExistingDayShortDistance() {
		Day from = Day.of(2008, Month.JANUARY, 3);
		Day to = Day.of(2008, Month.JANUARY, 5);
		List<ITransaction> byDate = TransactionProviderUtil.getTransactionsByDate(trList, from, to);
		assertEquals(2, byDate.size());
		assertEquals(Day.of(2008, Month.JANUARY, 4), byDate.get(0).getDay());
		assertEquals(Day.of(2008, Month.JANUARY, 4), byDate.get(1).getDay());
	}

	@Test
	public void notExistingDay() {
		Day from = Day.of(2008, Month.JANUARY, 3);
		Day to = Day.of(2008, Month.JANUARY, 25);
		List<ITransaction> byDate = TransactionProviderUtil.getTransactionsByDate(trList, from, to);
		assertEquals(4, byDate.size());
		assertEquals(Day.of(2008, Month.JANUARY, 4), byDate.get(0).getDay());
		assertEquals(Day.of(2008, Month.JANUARY, 4), byDate.get(1).getDay());
		assertEquals(Day.of(2008, Month.JANUARY, 6), byDate.get(2).getDay());
		assertEquals(Day.of(2008, Month.JANUARY, 6), byDate.get(3).getDay());
	}

	@Test
	public void beforeStartAndBehindEnd() throws Exception {
		Day from = Day.of(2008, Month.JANUARY, 1);
		Day to = Day.of(2008, Month.JANUARY, 30);
		List<ITransaction> byDate = TransactionProviderUtil.getTransactionsByDate(trList, from, to);
		assertEquals(trList.size(), byDate.size());
	}

	@Test
	public void startEnd() throws Exception {
		Day from = Day.of(2008, Month.JANUARY, 2);
		Day to = Day.of(2008, Month.JANUARY, 6);
		List<ITransaction> byDate = TransactionProviderUtil.getTransactionsByDate(trList, from, to);
		assertEquals(5, byDate.size());
	}
}
