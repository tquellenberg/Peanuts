package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.util.Day;

@XStreamAlias("bank-transaction")
public class BankTransaction extends LabeledTransaction {

	public BankTransaction(Day day, BigDecimal amount, String label) {
		super(day, amount, label);
	}

}
