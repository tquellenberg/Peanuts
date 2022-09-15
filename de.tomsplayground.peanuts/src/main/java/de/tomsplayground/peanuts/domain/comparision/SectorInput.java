package de.tomsplayground.peanuts.domain.comparision;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.util.Day;

public class SectorInput {
	
	private static final List<String> isins = Lists.newArrayList("XLC", "XLY", "XLP", "XLE", "XLF", "XLV", "XLI", "XLB", "XLRE", "XLK", "XLU");

	private static Day startDate = Day.of(2020, 1, 1);

	public static Comparison init(AccountManager accountManager) {
		Comparison comparisonInput = new Comparison();
		
		comparisonInput.setName("S+P 500 sectors");
		comparisonInput.setStartDate(startDate);
		
		List<Security> securities = accountManager.getSecurities().stream()
			.filter(s -> isins.contains(s.getISIN()))
			.collect(Collectors.toList());
		comparisonInput.setSecurities(securities);
		
		Optional<Security> baseSecuity = accountManager.getSecurities().stream()
			.filter(s -> StringUtils.equals(s.getISIN(), "SPY"))
			.findAny();
		comparisonInput.setBaseSecurity(baseSecuity.orElse(null));
		
		return comparisonInput;
	}

}
