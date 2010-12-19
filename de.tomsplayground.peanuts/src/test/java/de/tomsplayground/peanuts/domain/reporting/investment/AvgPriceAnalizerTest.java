package de.tomsplayground.peanuts.domain.reporting.investment;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.tomsplayground.peanuts.Helper;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.util.Day;

public class AvgPriceAnalizerTest {

	Security security = new Security("apple");

	private InvestmentTransaction buildTrade(BigDecimal price, BigDecimal quantity, BigDecimal commission, InvestmentTransaction.Type type) {
		return new InvestmentTransaction(new Day(), security, price, quantity,
			commission, type);
	}

	@Test
	public void multiBuyWithCommission() throws Exception {
		List<InvestmentTransaction> trans = new ArrayList<InvestmentTransaction>();
		trans.add(buildTrade(BigDecimal.TEN, new BigDecimal("2"), BigDecimal.ONE,
				InvestmentTransaction.Type.BUY));
		trans.add(buildTrade(new BigDecimal("12"), new BigDecimal("2"), BigDecimal.ONE,
				InvestmentTransaction.Type.BUY));
		IAnalyzer avgPrices = new AvgPriceAnalyzer(trans);
		
		List<AnalyzedInvestmentTransaction> analyzedTransactions = avgPrices.getAnalyzedTransactions();
		assertEquals(trans.size(), analyzedTransactions.size());
		AnalyzedInvestmentTransaction at = analyzedTransactions.get(0);
		Helper.assertEquals(new BigDecimal("10.50"), at.getAvgPrice());
		at = analyzedTransactions.get(1);
		Helper.assertEquals(new BigDecimal("11.50"), at.getAvgPrice());
	}
	
	@Test
	public void buyAndSellPartial() throws Exception {
		List<InvestmentTransaction> trans = new ArrayList<InvestmentTransaction>();
		trans.add(buildTrade(BigDecimal.TEN, new BigDecimal("2"), BigDecimal.ONE,
			InvestmentTransaction.Type.BUY));
		trans.add(buildTrade(new BigDecimal("11"), BigDecimal.ONE, BigDecimal.ONE, InvestmentTransaction.Type.SELL));
		IAnalyzer avgPrices = new AvgPriceAnalyzer(trans);
		
		List<AnalyzedInvestmentTransaction> analyzedTransactions = avgPrices.getAnalyzedTransactions();
		assertEquals(trans.size(), analyzedTransactions.size());
		AnalyzedInvestmentTransaction at = analyzedTransactions.get(0);
		Helper.assertEquals(new BigDecimal("10.50"), at.getAvgPrice());
		at = analyzedTransactions.get(1);
		Helper.assertEquals(new BigDecimal("10.50"), at.getAvgPrice());
	}
	
	@Test
	public void buyAndSellAll() throws Exception {
		List<InvestmentTransaction> trans = new ArrayList<InvestmentTransaction>();
		trans.add(buildTrade(BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE,
			InvestmentTransaction.Type.BUY));
		trans.add(buildTrade(new BigDecimal("11"), BigDecimal.ONE, BigDecimal.ONE, InvestmentTransaction.Type.SELL));
		IAnalyzer avgPrices = new AvgPriceAnalyzer(trans);

		List<AnalyzedInvestmentTransaction> analyzedTransactions = avgPrices.getAnalyzedTransactions();
		assertEquals(trans.size(), analyzedTransactions.size());
		AnalyzedInvestmentTransaction at = analyzedTransactions.get(0);
		Helper.assertEquals(new BigDecimal("11.00"), at.getAvgPrice());
		at = analyzedTransactions.get(1);
		Helper.assertEquals(new BigDecimal("0.00"), at.getAvgPrice());
	}
}
