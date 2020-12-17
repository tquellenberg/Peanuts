package de.tomsplayground.peanuts.domain.reporting.tax;

import java.math.BigDecimal;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction.Type;
import de.tomsplayground.peanuts.domain.reporting.investment.AnalyzedInvestmentTransaction;

public class RealizedGain {

	private final Inventory inventory;

	public RealizedGain(Inventory inventory) {
		this.inventory = inventory;
	}

	public ImmutableList<AnalyzedInvestmentTransaction> getRealizedTransaction(int year) {
		return inventory.getEntries().stream()
			.flatMap(e -> e.getTransactions().stream())
			.filter(t -> t.getType() == Type.SELL && t.getDay().year == year)
			.map(AnalyzedInvestmentTransaction.class::cast)
			.collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
	}

	public BigDecimal gain(int year) {
		return getRealizedTransaction(year).stream()
			.map(AnalyzedInvestmentTransaction::getGain)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
