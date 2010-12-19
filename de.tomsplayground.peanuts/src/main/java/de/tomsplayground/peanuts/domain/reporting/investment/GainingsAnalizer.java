package de.tomsplayground.peanuts.domain.reporting.investment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;

/**
 * 
 * Calculate realized gainings.
 * 
 */
public class GainingsAnalizer extends Analyzer {

	public GainingsAnalizer(List<? extends InvestmentTransaction> trans) {
		super(trans);
	}

	@Override
	public List<AnalyzedInvestmentTransaction> getAnalyzedTransactions() {
		List<AnalyzedInvestmentTransaction> result = new ArrayList<AnalyzedInvestmentTransaction>();
		for (InvestmentTransaction t : trans) {
			BigDecimal investValue = strategy.getInvestedAmount();
			strategy.buildBuyList(t);
			AnalyzedInvestmentTransaction at = AnalyzedInvestmentTransaction.createAnalyzedInvestmentTransaction(t);
			if (t.getType() == InvestmentTransaction.Type.SELL) {
				at.setGain(t.getAmount().subtract(investValue.subtract(strategy.getInvestedAmount())));
			}
			result.add(at);
		}
		return result;
	}

}
