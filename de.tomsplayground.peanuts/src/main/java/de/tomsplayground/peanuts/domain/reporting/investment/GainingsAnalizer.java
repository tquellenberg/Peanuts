package de.tomsplayground.peanuts.domain.reporting.investment;

import java.math.BigDecimal;

import com.google.common.base.Function;

import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;

/**
 * 
 * Calculate realized gainings.
 * 
 */
public class GainingsAnalizer extends Analyzer {

	private final Function<InvestmentTransaction, AnalyzedInvestmentTransaction> function = new Function<InvestmentTransaction, AnalyzedInvestmentTransaction>() {
		@Override
		public AnalyzedInvestmentTransaction apply(InvestmentTransaction t) {
			BigDecimal investValue = strategy.getInvestedAmount();
			strategy.buildBuyList(t);
			AnalyzedInvestmentTransaction at = AnalyzedInvestmentTransaction.createAnalyzedInvestmentTransaction(t);
			if (t.getType() == InvestmentTransaction.Type.SELL) {
				at.setGain(t.getAmount().subtract(investValue.subtract(strategy.getInvestedAmount())));
			}
			return at;
		}
	};

	@Override
	public Function<InvestmentTransaction, AnalyzedInvestmentTransaction> getFunction() {
		return function;
	}

}
