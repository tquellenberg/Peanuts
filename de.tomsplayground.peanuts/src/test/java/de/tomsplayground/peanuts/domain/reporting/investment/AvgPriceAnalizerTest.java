package de.tomsplayground.peanuts.domain.reporting.investment;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.Helper;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.util.Day;

public class AvgPriceAnalizerTest {

	Security security = new Security("apple");

	private InvestmentTransaction buildTrade(BigDecimal price, BigDecimal quantity, BigDecimal commission, InvestmentTransaction.Type type) {
		return new InvestmentTransaction(Day.today(), security, price, quantity, commission, type);
	}

	@Test
	public void multiBuyWithCommission() throws Exception {
		ImmutableList<InvestmentTransaction> trans = ImmutableList.of(
			buildTrade(BigDecimal.TEN, new BigDecimal("2"), BigDecimal.ONE, InvestmentTransaction.Type.BUY),
			buildTrade(new BigDecimal("12"), new BigDecimal("2"), BigDecimal.ONE, InvestmentTransaction.Type.BUY)
		);
		IAnalyzer avgPrices = new AvgPriceAnalyzer();

		List<AnalyzedInvestmentTransaction> analyzedTransactions = ImmutableList.copyOf(avgPrices.getAnalyzedTransactions(trans));

		assertEquals(trans.size(), analyzedTransactions.size());
		AnalyzedInvestmentTransaction at = analyzedTransactions.get(0);
		Helper.assertEquals(new BigDecimal("10.50"), at.getAvgPrice());
		at = analyzedTransactions.get(1);
		Helper.assertEquals(new BigDecimal("11.50"), at.getAvgPrice());
	}

	@Test
	public void buyAndSellPartial() throws Exception {
		ImmutableList<InvestmentTransaction> trans = ImmutableList.of(
			buildTrade(BigDecimal.TEN, new BigDecimal("2"), BigDecimal.ONE, InvestmentTransaction.Type.BUY),
			buildTrade(new BigDecimal("11"), BigDecimal.ONE, BigDecimal.ONE, InvestmentTransaction.Type.SELL)
		);
		IAnalyzer avgPrices = new AvgPriceAnalyzer();

		List<AnalyzedInvestmentTransaction> analyzedTransactions = ImmutableList.copyOf(avgPrices.getAnalyzedTransactions(trans));

		assertEquals(trans.size(), analyzedTransactions.size());
		AnalyzedInvestmentTransaction at = analyzedTransactions.get(0);
		Helper.assertEquals(new BigDecimal("10.50"), at.getAvgPrice());
		at = analyzedTransactions.get(1);
		Helper.assertEquals(new BigDecimal("10.50"), at.getAvgPrice());
	}

	@Test
	public void buyAndSellAll() throws Exception {
		ImmutableList<InvestmentTransaction> trans = ImmutableList.of(
			buildTrade(BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, InvestmentTransaction.Type.BUY),
			buildTrade(new BigDecimal("11"), BigDecimal.ONE, BigDecimal.ONE, InvestmentTransaction.Type.SELL)
		);
		IAnalyzer avgPrices = new AvgPriceAnalyzer();

		List<AnalyzedInvestmentTransaction> analyzedTransactions = ImmutableList.copyOf(avgPrices.getAnalyzedTransactions(trans));

		assertEquals(trans.size(), analyzedTransactions.size());
		AnalyzedInvestmentTransaction at = analyzedTransactions.get(0);
		Helper.assertEquals(new BigDecimal("11.00"), at.getAvgPrice());
		at = analyzedTransactions.get(1);
		Helper.assertEquals(new BigDecimal("0.00"), at.getAvgPrice());
	}

	@Test
	public void realValues() {
		ImmutableList<InvestmentTransaction> trans = ImmutableList.of(
			buildTrade(new BigDecimal("9.53"), new BigDecimal("52.466"), new BigDecimal("0"), InvestmentTransaction.Type.BUY),
			buildTrade(new BigDecimal("10.039"), new BigDecimal("49.806"), new BigDecimal("0"), InvestmentTransaction.Type.BUY),
			buildTrade(new BigDecimal("9.939"), new BigDecimal("50.307"), new BigDecimal("0"), InvestmentTransaction.Type.BUY)
		);
		IAnalyzer avgPrices = new AvgPriceAnalyzer();

		List<AnalyzedInvestmentTransaction> analyzedTransactions = ImmutableList.copyOf(avgPrices.getAnalyzedTransactions(trans));
		AnalyzedInvestmentTransaction at = analyzedTransactions.get(0);
		Helper.assertEquals(new BigDecimal("9.53"), at.getAvgPrice().setScale(2, RoundingMode.HALF_UP));
		Helper.assertEquals(new BigDecimal("500"), at.getInvestedAmount());
		at = analyzedTransactions.get(1);
		Helper.assertEquals(new BigDecimal("9.78"), at.getAvgPrice().setScale(2, RoundingMode.HALF_UP));
		Helper.assertEquals(new BigDecimal("1000"), at.getInvestedAmount());
		at = analyzedTransactions.get(2);
		Helper.assertEquals(new BigDecimal("9.83"), at.getAvgPrice().setScale(2, RoundingMode.HALF_UP));
		Helper.assertEquals(new BigDecimal("1500"), at.getInvestedAmount());

	}
}
