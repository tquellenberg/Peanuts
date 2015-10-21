package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Currency;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.base.Category;
import de.tomsplayground.peanuts.domain.base.CurrencyManager;
import de.tomsplayground.util.Day;

public class EuroTransactionWrapper implements ITransaction {

	private final ITransaction transaction;
	private final Currency currency;
	private BigDecimal euroAmount;

	public EuroTransactionWrapper(ITransaction transaction, Currency currency) {
		this.transaction = transaction;
		this.currency = currency;
	}

	@Override
	public BigDecimal getAmount() {
		if (euroAmount == null) {
			BigDecimal exchangeRate = (new CurrencyManager()).getExchangeRate(Currency.getInstance("EUR"), currency);
			euroAmount = transaction.getAmount().divide(exchangeRate, new MathContext(10, RoundingMode.HALF_EVEN));
		}
		return euroAmount;
	}

	@Override
	public Category getCategory() {
		return transaction.getCategory();
	}

	@Override
	public Day getDay() {
		return transaction.getDay();
	}

	@Override
	public String getMemo() {
		return transaction.getMemo();
	}

	@Override
	public boolean hasSplits() {
		return transaction.hasSplits();
	}

	@Override
	public ImmutableList<ITransaction> getSplits() {
		return ImmutableList.<ITransaction>copyOf(Collections2.transform(transaction.getSplits(), new Function<ITransaction, EuroTransactionWrapper>() {
			@Override
			public EuroTransactionWrapper apply(ITransaction input) {
				return new EuroTransactionWrapper(input, currency);
			}
		}));
	}

	@Override
	public void setCategory(Category categoryTo) {
		transaction.setCategory(categoryTo);
	}

}
