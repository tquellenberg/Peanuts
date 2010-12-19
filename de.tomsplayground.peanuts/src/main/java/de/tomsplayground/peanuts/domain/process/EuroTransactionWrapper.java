package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import de.tomsplayground.peanuts.domain.base.Category;
import de.tomsplayground.peanuts.domain.base.CurrencyManager;
import de.tomsplayground.util.Day;

public class EuroTransactionWrapper implements ITransaction {

	private ITransaction transaction;
	private Currency currency;
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
	public List<ITransaction> getSplits() {
		List<ITransaction> wrapped = new ArrayList<ITransaction>();
		for (ITransaction t :transaction.getSplits()) {
			wrapped.add(new EuroTransactionWrapper(t, currency));
		}
		return wrapped;
	}

	@Override
	public void setCategory(Category categoryTo) {
		transaction.setCategory(categoryTo);
	}

}
