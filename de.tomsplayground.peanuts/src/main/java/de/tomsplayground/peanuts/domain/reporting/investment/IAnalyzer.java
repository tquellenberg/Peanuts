package de.tomsplayground.peanuts.domain.reporting.investment;

import java.util.List;

public interface IAnalyzer {

	List<AnalyzedInvestmentTransaction> getAnalyzedTransactions();

}
