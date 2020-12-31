package de.tomsplayground.peanuts.domain.dividend;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.currenncy.Currencies;
import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;
import de.tomsplayground.peanuts.domain.currenncy.ExchangeRates;
import de.tomsplayground.peanuts.domain.process.IPriceProviderFactory;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.domain.query.InvestmentQuery;
import de.tomsplayground.peanuts.domain.reporting.investment.AnalyzerFactory;
import de.tomsplayground.peanuts.domain.reporting.transaction.Report;
import de.tomsplayground.util.Day;

public class DividendStats {

	private final Inventory fullInventory;

	private final ExchangeRates exchangeRates;

	private final Map<Day, List<Dividend>> groupedDividends;

	public DividendStats(AccountManager accountManager, IPriceProviderFactory priceProviderFactory) {
		Report report = new Report("temp");
		report.addQuery(new InvestmentQuery());
		report.setAccounts(accountManager.getAccounts());
		fullInventory = new Inventory(report, PriceProviderFactory.getInstance(), new Day(), new AnalyzerFactory());
		exchangeRates = new ExchangeRates(priceProviderFactory, accountManager);
		groupedDividends = accountManager.getSecurities().stream()
			.flatMap(s -> s.getDividends().stream())
			.filter(d -> getQuantity(d).signum() == 1)
			.collect(Collectors.groupingBy(d -> d.getPayDate().toMonth()));
	}

	public List<DividendMonth> getDividendMonths() {
		List<DividendMonth> result = groupedDividends.entrySet().stream()
			.map(e -> calcDividendMonth(e.getKey(), e.getValue()))
			.sorted()
			.collect(Collectors.toList());
		addYearlyStatsToMonth(result);
		return result;
	}

	public List<Dividend> getDividends(Day month) {
		if (groupedDividends.containsKey(month)) {
			return new ArrayList<>(groupedDividends.get(month));
		} else {
			return new ArrayList<>();
		}
	}

	private void addYearlyStatsToMonth(List<DividendMonth> result) {
		if (! result.isEmpty()) {
			int year = result.get(0).getMonth().year;
			BigDecimal yearlySum = BigDecimal.ZERO;
			BigDecimal yearlyNetto = BigDecimal.ZERO;
			BigDecimal futureYearlyAmount = BigDecimal.ZERO;
			for (DividendMonth dividendMonth : result) {
				if (dividendMonth.getMonth().year != year) {
					yearlySum = BigDecimal.ZERO;
					yearlyNetto = BigDecimal.ZERO;
					futureYearlyAmount = BigDecimal.ZERO;
					year = dividendMonth.getMonth().year;
				}
				yearlySum = yearlySum.add(dividendMonth.getAmountInDefaultCurrency());
				dividendMonth.setYearlyAmount(yearlySum);
				yearlyNetto = yearlyNetto.add(dividendMonth.getNettoInDefaultCurrency());
				dividendMonth.setYearlyNetto(yearlyNetto);
				BigDecimal futureAmountInDefaultCurrency = dividendMonth.getFutureAmountInDefaultCurrency();
				futureYearlyAmount = futureYearlyAmount.add(futureAmountInDefaultCurrency).add(dividendMonth.getAmountInDefaultCurrency());
				dividendMonth.setFutureYearlyAmount(futureYearlyAmount);
			}
		}
	}

	private DividendMonth calcDividendMonth(Day month, List<Dividend> dividends) {
		BigDecimal amountInDefaultCurrency = dividends.stream()
			.map(Dividend::getAmountInDefaultCurrency)
			.filter(Objects::nonNull)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal nettoInDefaultCurrency = dividends.stream()
			.map(Dividend::getNettoAmountInDefaultCurrency)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal futureAmountInDefaultCurrency = dividends.stream()
			.filter(d -> d.getAmountInDefaultCurrency() == null)
			.map(this::futureDividend)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		return new DividendMonth(month, amountInDefaultCurrency, nettoInDefaultCurrency, futureAmountInDefaultCurrency);
	}

	public BigDecimal getQuantity(Dividend entry) {
		if (entry.getQuantity() != null) {
			return entry.getQuantity();
		}
		synchronized (fullInventory) {
			fullInventory.setDate(entry.getPayDate());
			return fullInventory.getInventoryEntry(entry.getSecurity()).getQuantity();
		}
	}

	private BigDecimal futureDividend(Dividend d) {
		BigDecimal quantity = getQuantity(d);
		if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO;
		}
		BigDecimal amountPerShare = d.getAmountPerShare();
		if (amountPerShare.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO;
		}
		BigDecimal amount = quantity.multiply(amountPerShare);
		if (d.getCurrency().equals(Currencies.getInstance().getDefaultCurrency())) {
			return amount;
		}
		CurrencyConverter converter = exchangeRates.createCurrencyConverter(d.getCurrency(), Currencies.getInstance().getDefaultCurrency());
		if (converter != null) {
			amount = converter.convert(amount, d.getPayDate());
			return amount;
		}
		return BigDecimal.ZERO;
	}
}
