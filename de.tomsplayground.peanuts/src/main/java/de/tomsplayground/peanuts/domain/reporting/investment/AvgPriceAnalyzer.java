package de.tomsplayground.peanuts.domain.reporting.investment;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.google.common.base.Function;

import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;

public class AvgPriceAnalyzer extends Analyzer {

	private final Function<InvestmentTransaction, AnalyzedInvestmentTransaction> function = new Function<InvestmentTransaction, AnalyzedInvestmentTransaction>() {
		@Override
		public AnalyzedInvestmentTransaction apply(InvestmentTransaction t) {
			strategy.buildBuyList(t);
			AnalyzedInvestmentTransaction at = AnalyzedInvestmentTransaction.createAnalyzedInvestmentTransaction(t);
			if (strategy.getQuantity().signum() == 0)
				at.setAvgPrice(BigDecimal.ZERO);
			else
				at.setAvgPrice(strategy.getInvestedAmount().divide(strategy.getQuantity(), RoundingMode.HALF_EVEN));
			return at;
		}
	};

	@Override
	public Function<InvestmentTransaction, AnalyzedInvestmentTransaction> getFunction() {
		return function;
	}

}
