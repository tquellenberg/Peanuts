package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;
import java.math.RoundingMode;

import de.tomsplayground.peanuts.domain.base.Category;
import de.tomsplayground.peanuts.domain.base.CurrencyManager;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class Transfer {

	private final TransferTransaction transTo;
	private final TransferTransaction transFrom;

	public Transfer(ITransferLocation source, ITransferLocation target, BigDecimal value,
		Day date) {
		if (source == target) {
			throw new IllegalStateException();
		}
		transFrom = new TransferTransaction(date, value.negate(), target, true);
		if ( !target.getCurrency().equals(source.getCurrency())) {
			BigDecimal exchangeRate = new CurrencyManager().getExchangeRate(source.getCurrency(),
				target.getCurrency());
			value = value.multiply(exchangeRate, PeanutsUtil.MC);
			value = value.setScale(2, RoundingMode.HALF_UP);
		}
		transTo = new TransferTransaction(date, value, source, false);
		transTo.setComplement(transFrom);
		transFrom.setComplement(transTo);
	}

	public Transaction getTransferTo() {
		return transTo;
	}

	public Transaction getTransferFrom() {
		return transFrom;
	}

	public void setMemo(String memo) {
		transFrom.setMemo(memo);
		transTo.setMemo(memo);
	}

	public void setLabel(String label) {
		transFrom.setLabel(label);
		transTo.setLabel(label);
	}

	public void setCategory(Category category) {
		transFrom.setCategory(category);
		transTo.setCategory(category);
	}

}
