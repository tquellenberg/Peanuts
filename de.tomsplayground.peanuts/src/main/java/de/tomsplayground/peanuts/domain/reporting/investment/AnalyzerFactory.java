package de.tomsplayground.peanuts.domain.reporting.investment;

import java.util.List;

import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;

public class AnalyzerFactory {

	public IAnalyzer getAnalizer(List<InvestmentTransaction> trans) {
		return new CombinedAnalyzer(trans);
	}

}
