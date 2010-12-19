package de.tomsplayground.peanuts.domain.reporting.investment;

import java.util.List;

import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;

public class CombinedAnalyzer implements IAnalyzer {

	final private List<AnalyzedInvestmentTransaction> analyzedTransactions;
	
	public CombinedAnalyzer(List<InvestmentTransaction> trans) {
		List<AnalyzedInvestmentTransaction> ats = new GainingsAnalizer(trans).getAnalyzedTransactions();
		ats = new AvgPriceAnalyzer(ats).getAnalyzedTransactions();
		ats = new QuantitySumAnalyzer(ats).getAnalyzedTransactions();
		analyzedTransactions = ats;
	}
	
	@Override
	public List<AnalyzedInvestmentTransaction> getAnalyzedTransactions() {
		return analyzedTransactions;
	}

}
