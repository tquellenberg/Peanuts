package de.tomsplayground.peanuts.domain.reporting.investment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;

public class AvgPriceAnalyzer extends Analyzer {

	public AvgPriceAnalyzer(List<? extends InvestmentTransaction> trans) {
		super(trans);
	}

	@Override
	public List<AnalyzedInvestmentTransaction> getAnalyzedTransactions() {
		List<AnalyzedInvestmentTransaction> result = new ArrayList<AnalyzedInvestmentTransaction>();
		for (InvestmentTransaction t : trans) {
			strategy.buildBuyList(t);
			AnalyzedInvestmentTransaction at = AnalyzedInvestmentTransaction.createAnalyzedInvestmentTransaction(t);
			if (strategy.getQuantity().signum() == 0)
				at.setAvgPrice(BigDecimal.ZERO);
			else
				at.setAvgPrice(strategy.getInvestedAmount().divide(strategy.getQuantity(), RoundingMode.HALF_EVEN));
			result.add(at);
		}
		return result;
	}

}
