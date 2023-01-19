package de.tomsplayground.peanuts.domain.comparision;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.util.Day;

public class DefaultComparisonInput {
	
	private static final List<String> S_AND_P_ISIN = List.of("XLC", "XLY", "XLP", "XLE", "XLF", "XLV", "XLI", "XLB", "XLRE", "XLK", "XLU");
	private static final List<String> CURRENCY_ISIN = List.of("EURUSD", "EURGBP", "EURCHF", "EURJPY", "EURAUD", "EURZAR", "EURSGD");

	private static Day startDate = Day.firstDayOfYear(2020);

	public static List<Comparison> init(AccountManager accountManager) {
		List<Comparison> defaultComparisons = new ArrayList<>();
		
		defaultComparisons.add(sandp500(accountManager));
		defaultComparisons.add(goldsilver(accountManager));
		defaultComparisons.add(currencies(accountManager));
		
		return defaultComparisons;
	}

	private static Comparison goldsilver(AccountManager accountManager) {
		Comparison comparisonInput = new Comparison();
		
		comparisonInput.setName("Gold Silver");
		comparisonInput.setStartDate(startDate);
		
		Optional<Security> securities = accountManager.getSecurities().stream()
				.filter(s -> StringUtils.equals(s.getISIN(), "SILBER"))
				.findAny();
		comparisonInput.setSecurities(securities.stream().toList());
		
		Optional<Security> baseSecuity = accountManager.getSecurities().stream()
				.filter(s -> StringUtils.equals(s.getISIN(), "GOLD"))
				.findAny();
		comparisonInput.setBaseSecurity(baseSecuity.orElse(null));
		
		return comparisonInput;
	}
	
	private static Comparison currencies(AccountManager accountManager) {
		Comparison comparisonInput = new Comparison();
		
		comparisonInput.setName("Currencies");
		comparisonInput.setStartDate(startDate);
		
		List<Security> securities = accountManager.getSecurities().stream()
			.filter(s -> s.getISIN() != null)
			.filter(s -> CURRENCY_ISIN.contains(s.getISIN()))
			.toList();
		comparisonInput.setSecurities(securities);
		
		return comparisonInput;
	}

	private static Comparison sandp500(AccountManager accountManager) {
		Comparison comparisonInput = new Comparison();
		
		comparisonInput.setName("S+P 500 sectors");
		comparisonInput.setStartDate(startDate);
		
		List<Security> securities = accountManager.getSecurities().stream()
			.filter(s -> s.getISIN() != null)
			.filter(s -> S_AND_P_ISIN.contains(s.getISIN()))
			.toList();
		comparisonInput.setSecurities(securities);
		
		Optional<Security> baseSecuity = accountManager.getSecurities().stream()
			.filter(s -> StringUtils.equals(s.getISIN(), "SPY"))
			.findAny();
		comparisonInput.setBaseSecurity(baseSecuity.orElse(null));
		
		return comparisonInput;
	}

}
