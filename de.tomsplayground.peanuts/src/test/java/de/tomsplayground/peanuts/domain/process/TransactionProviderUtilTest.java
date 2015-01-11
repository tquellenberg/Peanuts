package de.tomsplayground.peanuts.domain.process;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.base.ITransactionProvider;
import de.tomsplayground.peanuts.domain.base.TransactionProviderUtil;
import de.tomsplayground.util.Day;


public class TransactionProviderUtilTest {

	ITransactionProvider defaultprovider = new ITransactionProvider() {
		@Override
		public ImmutableList<ITransaction> getTransactions() {
			ImmutableList<ITransaction> trList = ImmutableList.<ITransaction>of(
				new BankTransaction(new Day(2008, 0, 2), BigDecimal.ZERO, ""),
				new BankTransaction(new Day(2008, 0, 4), BigDecimal.ZERO, ""),
				new BankTransaction(new Day(2008, 0, 4), BigDecimal.ZERO, ""),
				new BankTransaction(new Day(2008, 0, 6), BigDecimal.ZERO, ""),
				new BankTransaction(new Day(2008, 0, 6), BigDecimal.ZERO, ""));
			return trList;
		}
		@Override
		public ImmutableList<ITransaction> getTransactionsByDate(Day from, Day to) {
			return null;
		}
		@Override
		public Day getMaxDate() {
			return new Day(2008, 0, 6);
		}
		@Override
		public Day getMinDate() {
			return new Day(2008, 0, 2);
		}
		@Override
		public BigDecimal getBalance(Day date) {
			return BigDecimal.ZERO;
		}
		@Override
		public java.util.Currency getCurrency() {
			return null;
		}
	};

	@Test
	public void emptylist() throws Exception {
		ITransactionProvider provider = new ITransactionProvider() {
			@Override
			public ImmutableList<ITransaction> getTransactions() {
				return ImmutableList.of();
			}
			@Override
			public ImmutableList<ITransaction> getTransactionsByDate(Day from, Day to) {
				return null;
			}
			@Override
			public Day getMaxDate() {
				return null;
			}
			@Override
			public Day getMinDate() {
				return null;
			}
			@Override
			public BigDecimal getBalance(Day date) {
				return BigDecimal.ZERO;
			}
			@Override
			public java.util.Currency getCurrency() {
				return null;
			}
		};
		Day from = new Day(2008, 0, 2);
		Day to = new Day(2008, 0, 12);
		List<ITransaction> byDate = TransactionProviderUtil.getTransactionsByDate(provider, from, to);
		assertTrue(byDate.isEmpty());
	}

	@Test
	public void allNull() throws Exception {
		List<ITransaction> byDate = TransactionProviderUtil.getTransactionsByDate(defaultprovider, null, null);
		assertEquals(5, byDate.size());
	}

	@Test
	public void oneDay() {
		Day from = new Day(2008, 0, 4);
		Day to = from;
		List<ITransaction> byDate = TransactionProviderUtil.getTransactionsByDate(defaultprovider, from, to);
		assertEquals(2, byDate.size());
		assertEquals(from, byDate.get(0).getDay());
		assertEquals(from, byDate.get(1).getDay());
	}

	@Test
	public void notExactDay() {
		Day from = new Day(2008, 0, 3);
		Day to = new Day(2008, 0, 5);
		List<ITransaction> byDate = TransactionProviderUtil.getTransactionsByDate(defaultprovider, from, to);
		assertEquals(2, byDate.size());
		assertEquals(new Day(2008, 0, 4), byDate.get(0).getDay());
		assertEquals(new Day(2008, 0, 4), byDate.get(1).getDay());
	}

	@Test
	public void beforeStartAndBehindEnd() throws Exception {
		Day from = new Day(2008, 0, 1);
		Day to = new Day(2008, 0, 7);
		List<ITransaction> byDate = TransactionProviderUtil.getTransactionsByDate(defaultprovider, from, to);
		assertEquals(5, byDate.size());
	}

	@Test
	public void startEnd() throws Exception {
		Day from = new Day(2008, 0, 2);
		Day to = new Day(2008, 0, 6);
		List<ITransaction> byDate = TransactionProviderUtil.getTransactionsByDate(defaultprovider, from, to);
		assertEquals(5, byDate.size());
	}
}
