package de.tomsplayground.peanuts.domain.reporting.investment;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import com.google.common.base.Function;

import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;

public class AvgPriceAnalyzer extends Analyzer {

	private static final MathContext MC = new MathContext(10, RoundingMode.HALF_EVEN);

	private final Function<InvestmentTransaction, AnalyzedInvestmentTransaction> function = new Function<InvestmentTransaction, AnalyzedInvestmentTransaction>() {
		@Override
		public AnalyzedInvestmentTransaction apply(InvestmentTransaction t) {
			strategy.buildBuyList(t);
			AnalyzedInvestmentTransaction at = AnalyzedInvestmentTransaction.createAnalyzedInvestmentTransaction(t);
			if (strategy.getQuantity().signum() == 0) {
				at.setAvgPrice(BigDecimal.ZERO);
			} else {
				at.setAvgPrice(strategy.getInvestedAmount().divide(strategy.getQuantity(), MC));
			}
			at.setInvestedAmount(strategy.getInvestedAmount());
			return at;
		}
	};

	@Override
	public Function<InvestmentTransaction, AnalyzedInvestmentTransaction> getFunction() {
		return function;
	}

}
