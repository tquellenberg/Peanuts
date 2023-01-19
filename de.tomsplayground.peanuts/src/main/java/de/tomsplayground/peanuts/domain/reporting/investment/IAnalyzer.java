package de.tomsplayground.peanuts.domain.reporting.investment;

import java.util.Collection;
import java.util.List;

import com.google.common.base.Function;

import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;


public interface IAnalyzer {

	Function<InvestmentTransaction, AnalyzedInvestmentTransaction> getFunction();

	default List<AnalyzedInvestmentTransaction> getAnalyzedTransactions(Collection<? extends InvestmentTransaction> trans) {
		return trans.stream()
			.map(getFunction())
			.toList();
	}

}
