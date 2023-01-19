package de.tomsplayground.peanuts.domain.reporting.investment;

public abstract class Analyzer implements IAnalyzer {

	protected final Strategy strategy = new Fifo();


}