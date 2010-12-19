package de.tomsplayground.peanuts.domain.reporting.investment;

import java.util.List;

import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;

public abstract class Analyzer implements IAnalyzer {

	protected final List<? extends InvestmentTransaction> trans;
	protected final Strategy strategy = new Fifo();
	
	public Analyzer(List<? extends InvestmentTransaction> trans) {
		this.trans = trans;
	}

}