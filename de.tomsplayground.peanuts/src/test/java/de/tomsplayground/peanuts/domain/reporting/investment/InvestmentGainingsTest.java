package de.tomsplayground.peanuts.domain.reporting.investment;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.Helper;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.util.Day;


public class InvestmentGainingsTest {

	Security security = new Security("apple");

	private InvestmentTransaction buildTrade(BigDecimal price, BigDecimal quantity,
		BigDecimal commission, InvestmentTransaction.Type type) {
		return new InvestmentTransaction(Day.today(), security, price, quantity,
			commission, type);
	}

	@Test
	public void testFullBuySell() throws Exception {
		List<InvestmentTransaction> trans = new ArrayList<InvestmentTransaction>();
		trans.add(buildTrade(new BigDecimal("10.00"), BigDecimal.ONE, BigDecimal.ZERO,
			InvestmentTransaction.Type.BUY));
		InvestmentTransaction sellTransaction = buildTrade(new BigDecimal("15.00"), BigDecimal.ONE,
			BigDecimal.ZERO, InvestmentTransaction.Type.SELL);
		trans.add(sellTransaction);
		GainingsAnalizer gainings = new GainingsAnalizer();

		List<AnalyzedInvestmentTransaction> analyzedTransactions = ImmutableList.copyOf(gainings.getAnalyzedTransactions(trans));

		assertEquals(trans.size(), analyzedTransactions.size());
		AnalyzedInvestmentTransaction at = analyzedTransactions.get(1);
		Helper.assertEquals(new BigDecimal("5.00"), at.getGain());
	}

	@Test
	public void testFirstInFirstOut() throws Exception {
		List<InvestmentTransaction> trans = new ArrayList<InvestmentTransaction>();
		trans.add(buildTrade(new BigDecimal("10.00"), BigDecimal.ONE, BigDecimal.ZERO,
			InvestmentTransaction.Type.BUY));
		trans.add(buildTrade(new BigDecimal("15.00"), BigDecimal.ONE, BigDecimal.ZERO,
			InvestmentTransaction.Type.BUY));
		InvestmentTransaction sellTransaction = buildTrade(new BigDecimal("15.00"), BigDecimal.ONE,
			BigDecimal.ZERO, InvestmentTransaction.Type.SELL);
		trans.add(sellTransaction);
		GainingsAnalizer gainings = new GainingsAnalizer();

		List<AnalyzedInvestmentTransaction> analyzedTransactions = ImmutableList.copyOf(gainings.getAnalyzedTransactions(trans));

		AnalyzedInvestmentTransaction at = analyzedTransactions.get(2);
		assertEquals(0, at.getGain().compareTo(new BigDecimal("5.00")));
	}

	@Test
	public void testPartialSell() throws Exception {
		List<InvestmentTransaction> trans = new ArrayList<InvestmentTransaction>();
		trans.add(buildTrade(new BigDecimal("10.00"), BigDecimal.TEN, BigDecimal.ZERO,
			InvestmentTransaction.Type.BUY));
		InvestmentTransaction sellTransaction = buildTrade(new BigDecimal("15.00"), BigDecimal.ONE,
			BigDecimal.ZERO, InvestmentTransaction.Type.SELL);
		trans.add(sellTransaction);
		GainingsAnalizer gainings = new GainingsAnalizer();

		List<AnalyzedInvestmentTransaction> analyzedTransactions = ImmutableList.copyOf(gainings.getAnalyzedTransactions(trans));

		AnalyzedInvestmentTransaction at = analyzedTransactions.get(1);
		assertEquals(0, at.getGain().compareTo(new BigDecimal("5.00")));
	}

	@Test
	public void testPartialSellWithCommission() throws Exception {
		List<InvestmentTransaction> trans = new ArrayList<InvestmentTransaction>();
		trans.add(buildTrade(new BigDecimal("10.00"), BigDecimal.TEN, BigDecimal.TEN,
			InvestmentTransaction.Type.BUY));
		InvestmentTransaction sellTransaction = buildTrade(new BigDecimal("15.00"), BigDecimal.ONE,
			BigDecimal.ONE, InvestmentTransaction.Type.SELL);
		trans.add(sellTransaction);
		GainingsAnalizer gainings = new GainingsAnalizer();

		List<AnalyzedInvestmentTransaction> analyzedTransactions = ImmutableList.copyOf(gainings.getAnalyzedTransactions(trans));

		AnalyzedInvestmentTransaction at = analyzedTransactions.get(1);
		assertEquals(0, at.getGain().compareTo(new BigDecimal("3.00")));
	}

	@Test
	public void testAlreadySelled() throws Exception {
		List<InvestmentTransaction> trans = new ArrayList<InvestmentTransaction>();
		// Buy 1 for 10
		trans.add(buildTrade(new BigDecimal("10.00"), BigDecimal.ONE, BigDecimal.ZERO,
			InvestmentTransaction.Type.BUY));
		// Buy 1 for 15
		trans.add(buildTrade(new BigDecimal("15.00"), BigDecimal.ONE, BigDecimal.ZERO,
			InvestmentTransaction.Type.BUY));
		// Sell 1 for 15
		InvestmentTransaction sellTransaction1 = buildTrade(new BigDecimal("15.00"),
			BigDecimal.ONE, BigDecimal.ZERO, InvestmentTransaction.Type.SELL);
		trans.add(sellTransaction1);
		// Sell 1 for 15
		InvestmentTransaction sellTransaction2 = buildTrade(new BigDecimal("15.00"),
			BigDecimal.ONE, BigDecimal.ZERO, InvestmentTransaction.Type.SELL);
		trans.add(sellTransaction2);
		GainingsAnalizer gainings = new GainingsAnalizer();

		List<AnalyzedInvestmentTransaction> analyzedTransactions = ImmutableList.copyOf(gainings.getAnalyzedTransactions(trans));

		AnalyzedInvestmentTransaction at1 = analyzedTransactions.get(2);
		assertEquals(0, at1.getGain().compareTo(new BigDecimal("5.00")));
		AnalyzedInvestmentTransaction at2 = analyzedTransactions.get(3);
		assertEquals(0, at2.getGain().compareTo(new BigDecimal("0.00")));
	}

	@Test
	public void testPartialSelled() throws Exception {
		List<InvestmentTransaction> trans = new ArrayList<InvestmentTransaction>();
		// Buy 10 for 10
		trans.add(buildTrade(new BigDecimal("10.00"), BigDecimal.TEN, BigDecimal.ZERO,
			InvestmentTransaction.Type.BUY));
		// Buy 1 for 15
		trans.add(buildTrade(new BigDecimal("15.00"), BigDecimal.ONE, BigDecimal.ZERO,
			InvestmentTransaction.Type.BUY));
		// Sell 7 for 15
		InvestmentTransaction sellTransaction1 = buildTrade(new BigDecimal("15.00"),
			new BigDecimal("7"), BigDecimal.ZERO, InvestmentTransaction.Type.SELL);
		trans.add(sellTransaction1);
		// Sell 4 for 15
		InvestmentTransaction sellTransaction2 = buildTrade(new BigDecimal("15.00"),
			new BigDecimal("4"), BigDecimal.ZERO, InvestmentTransaction.Type.SELL);
		trans.add(sellTransaction2);
		GainingsAnalizer gainings = new GainingsAnalizer();

		List<AnalyzedInvestmentTransaction> analyzedTransactions = ImmutableList.copyOf(gainings.getAnalyzedTransactions(trans));

		AnalyzedInvestmentTransaction at1 = analyzedTransactions.get(2);
		assertEquals(0, at1.getGain().compareTo(new BigDecimal("35.00")));
		AnalyzedInvestmentTransaction at2 = analyzedTransactions.get(3);
		assertEquals(0, at2.getGain().compareTo(new BigDecimal("15.00")));
	}

}
