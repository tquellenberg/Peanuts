package de.tomsplayground.peanuts.domain.dividend;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.util.Day;

public class DividendStats {

	private final AccountManager accountManager;

	public DividendStats(AccountManager accountManager) {
		this.accountManager = accountManager;
	}

	public List<DividendMonth> getDividendMonths() {
		Map<Day, List<Dividend>> groupedDividends = accountManager.getSecurities().stream()
			.flatMap(s -> s.getDividends().stream())
			.collect(Collectors.groupingBy(d -> d.getPayDate().toMonth()));
		List<DividendMonth> result = groupedDividends.entrySet().stream()
			.map(e -> calcDividendMonth(e.getKey(), e.getValue()))
			.sorted()
			.collect(Collectors.toList());
		if (! result.isEmpty()) {
			int year = result.get(0).getMonth().year;
			BigDecimal yearlySum = BigDecimal.ZERO;
			BigDecimal yearlyNetto = BigDecimal.ZERO;
			for (DividendMonth dividendMonth : result) {
				if (dividendMonth.getMonth().year != year) {
					yearlySum = BigDecimal.ZERO;
					yearlyNetto = BigDecimal.ZERO;
					year = dividendMonth.getMonth().year;
				}
				yearlySum = yearlySum.add(dividendMonth.getAmountInDefaultCurrency());
				dividendMonth.setYearlyAmount(yearlySum);
				yearlyNetto = yearlyNetto.add(dividendMonth.getNettoInDefaultCurrency());
				dividendMonth.setYearlyNetto(yearlyNetto);
			}
		}
		return result;
	}

	private DividendMonth calcDividendMonth(Day month, List<Dividend> dividends) {
		BigDecimal amountInDefaultCurrency = dividends.stream()
			.map(Dividend::getAmountInDefaultCurrency)
			.filter(Objects::nonNull)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal nettoInDefaultCurrency = dividends.stream()
			.map(Dividend::getNettoAmountInDefaultCurrency)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		return new DividendMonth(month, amountInDefaultCurrency, nettoInDefaultCurrency);
	}
}
