package de.tomsplayground.peanuts.domain.reporting.investment;

import com.google.common.collect.Iterables;

import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;


public abstract class Analyzer implements IAnalyzer {

	protected final Strategy strategy = new Fifo();
		
	@Override
	public Iterable<AnalyzedInvestmentTransaction> getAnalyzedTransactions(Iterable<? extends InvestmentTransaction> trans) {
		return Iterables.transform(trans, getFunction());
	}

}