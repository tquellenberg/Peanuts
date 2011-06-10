package de.tomsplayground.peanuts.domain.reporting.investment;


public class AnalyzerFactory {

	public IAnalyzer getAnalizer() {
		return new CombinedAnalyzer();
	}

}
