package de.tomsplayground.peanuts.domain.reporting.investment;

import com.google.common.base.Function;

import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;

public class QuantitySumAnalyzer extends Analyzer {

	private final Function<InvestmentTransaction, AnalyzedInvestmentTransaction> function = new Function<InvestmentTransaction, AnalyzedInvestmentTransaction>() {
		@Override
		public AnalyzedInvestmentTransaction apply(InvestmentTransaction t) {
			strategy.buildBuyList(t);
			AnalyzedInvestmentTransaction at = AnalyzedInvestmentTransaction.createAnalyzedInvestmentTransaction(t);
			at.setQuantitySum(strategy.getQuantity());
			return at;
		}
	};

	@Override
	public Function<InvestmentTransaction, AnalyzedInvestmentTransaction> getFunction() {
		return function;
	}

}
