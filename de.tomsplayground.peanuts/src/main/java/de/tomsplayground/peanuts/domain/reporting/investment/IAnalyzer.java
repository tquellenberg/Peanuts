package de.tomsplayground.peanuts.domain.reporting.investment;

import com.google.common.base.Function;

import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;


public interface IAnalyzer {

	Function<InvestmentTransaction, AnalyzedInvestmentTransaction> getFunction();
	
	Iterable<AnalyzedInvestmentTransaction> getAnalyzedTransactions(Iterable<? extends InvestmentTransaction> trans);

}
