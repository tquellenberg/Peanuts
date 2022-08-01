package de.tomsplayground.peanuts.domain.dividend;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.currenncy.Currencies;
import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;
import de.tomsplayground.peanuts.domain.currenncy.ExchangeRates;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class SecurityDividendStats {

	private ImmutableList<Dividend> dividends;

	public SecurityDividendStats(Security security) {
		dividends = security.getDividends();
	}
	
	public BigDecimal getYearlyDividendSum(int year) {
		 BigDecimal dividendSum = dividends.stream()
			.filter(d -> d.getPayDate().year == year)
			.map(d -> d.getAmountPerShare())
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		return dividendSum;
	}
	
	public BigDecimal getFutureDividendSum(BigDecimal quantity, ExchangeRates exchangeRates) {
		Day from = Day.today();
		Day to = from.addYear(1);
		return dividends.stream()
			.filter(d-> d.getPayDate().after(from) && d.getPayDate().beforeOrEquals(to))
			.map(d -> sumInDefaultCurrency(d, quantity, exchangeRates))
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
	
	public BigDecimal sumInDefaultCurrency(Dividend d, BigDecimal quantity, ExchangeRates exchangeRates) {
		BigDecimal amount = quantity.multiply(d.getAmountPerShare());

		CurrencyConverter converter = exchangeRates
				.createCurrencyConverter(d.getCurrency(), Currencies.getInstance().getDefaultCurrency());
		amount = converter.convert(amount, d.getPayDate());

		return amount;
	}
	
	public BigDecimal getLatestPayedDividendSum() {
		 List<Dividend> payed = dividends.stream()
			.filter(d -> d.getAmountInDefaultCurrency() != null)
			.sorted()
			.collect(Collectors.toList());

		 BigDecimal dividendSum = BigDecimal.ZERO;
		 if (! payed.isEmpty()) {
			 Day begin = payed.get(payed.size()-1).getPayDate().addDays(- (365 - 20));
			 dividendSum = payed.stream()
			 	.filter(d -> d.getPayDate().after(begin))
			 	.map(d -> d.getAmountInDefaultCurrency())
			 	.reduce(BigDecimal.ZERO, BigDecimal::add);
		 }
		 
		return dividendSum;
	}
	
	public BigDecimal getLatestPayedDividendPerShare(DividendStats dividendStats) {
		 List<Dividend> payed = dividends.stream()
			.filter(d -> d.getAmountInDefaultCurrency() != null)
			.sorted()
			.collect(Collectors.toList());

		 BigDecimal dividendSum = BigDecimal.ZERO;
		 if (! payed.isEmpty()) {
			 Day begin = payed.get(payed.size()-1).getPayDate().addDays(- (365 - 20));
			 dividendSum = payed.stream()
			 	.filter(d -> d.getPayDate().after(begin))
			 	.map(d -> dividendPerShare(d, dividendStats))
			 	.reduce(BigDecimal.ZERO, BigDecimal::add);
		 }
		 
		return dividendSum;
	}
	
	private BigDecimal dividendPerShare(Dividend d, DividendStats ds) {
		BigDecimal amount = d.getAmountInDefaultCurrency();
		if (amount.signum() == 0) {
			return BigDecimal.ZERO;
		}
		BigDecimal quantity = ds.getQuantity(d);
		if (quantity.signum() == 0) {
			return BigDecimal.ZERO;
		}
		return amount.divide(quantity, PeanutsUtil.MC);		
	}
	
	public BigDecimal getYoC(BigDecimal avgPricePerShare, DividendStats dividendStats) {
		BigDecimal dividendPerShare = getLatestPayedDividendPerShare(dividendStats);
		if (dividendPerShare.signum() == 0) {
			return BigDecimal.ZERO;
		}
		if (avgPricePerShare.signum() == 0) {
			return BigDecimal.ZERO;
		}
		return dividendPerShare.divide(avgPricePerShare, PeanutsUtil.MC);
	}
}
