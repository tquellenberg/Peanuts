package de.tomsplayground.peanuts.domain.reporting.investment;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;

public class CombinedAnalyzer implements IAnalyzer {

	private GainingsAnalizer gainingsAnalizer;
	private AvgPriceAnalyzer avgPriceAnalyzer;
	private QuantitySumAnalyzer quantitySumAnalyzer;

	private final Function<InvestmentTransaction, AnalyzedInvestmentTransaction> function = new Function<InvestmentTransaction, AnalyzedInvestmentTransaction>() {
		@Override
		public AnalyzedInvestmentTransaction apply(InvestmentTransaction input) {
			AnalyzedInvestmentTransaction apply = gainingsAnalizer.getFunction().apply(input);
			apply = avgPriceAnalyzer.getFunction().apply(apply);
			apply = quantitySumAnalyzer.getFunction().apply(apply);
			return apply;
		}
	};

	public CombinedAnalyzer() {
		gainingsAnalizer = new GainingsAnalizer();
		avgPriceAnalyzer = new AvgPriceAnalyzer();
		quantitySumAnalyzer = new QuantitySumAnalyzer();
	}

	@Override
	public Function<InvestmentTransaction, AnalyzedInvestmentTransaction> getFunction() {
		return function;
	}

	@Override
	public Iterable<AnalyzedInvestmentTransaction> getAnalyzedTransactions(Iterable<? extends InvestmentTransaction> trans) {
		return Iterables.transform(trans, getFunction());
	}

}
