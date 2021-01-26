package de.tomsplayground.peanuts.domain.reporting.investment;

import java.math.BigDecimal;

import com.google.common.base.Function;

import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class AvgPriceAnalyzer extends Analyzer {

	private final Function<InvestmentTransaction, AnalyzedInvestmentTransaction> function = new Function<InvestmentTransaction, AnalyzedInvestmentTransaction>() {
		@Override
		public AnalyzedInvestmentTransaction apply(InvestmentTransaction t) {
			strategy.buildBuyList(t);
			AnalyzedInvestmentTransaction at = AnalyzedInvestmentTransaction.createAnalyzedInvestmentTransaction(t);
			if (strategy.getQuantity().signum() == 0) {
				at.setAvgPrice(BigDecimal.ZERO);
			} else {
				at.setAvgPrice(strategy.getInvestedAmount().divide(strategy.getQuantity(), PeanutsUtil.MC));
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
