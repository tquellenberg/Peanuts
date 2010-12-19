package de.tomsplayground.peanuts.domain.reporting.investment;

import java.util.ArrayList;
import java.util.List;

import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;

public class QuantitySumAnalyzer extends Analyzer {

	public QuantitySumAnalyzer(List<? extends InvestmentTransaction> trans) {
		super(trans);
	}

	@Override
	public List<AnalyzedInvestmentTransaction> getAnalyzedTransactions() {
		List<AnalyzedInvestmentTransaction> result = new ArrayList<AnalyzedInvestmentTransaction>();
		for (InvestmentTransaction t : trans) {
			strategy.buildBuyList(t);
			AnalyzedInvestmentTransaction at = AnalyzedInvestmentTransaction.createAnalyzedInvestmentTransaction(t);
			at.setQuantitySum(strategy.getQuantity());
			result.add(at);
		}
		return result;
	}

}
