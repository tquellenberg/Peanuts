package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;

import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.INamedElement;
import de.tomsplayground.util.Day;

public interface ICredit extends INamedElement {

	public Day getStart();

	public Day getEnd();

	public BigDecimal amount(Day day);

	public BigDecimal getInterest(Day day);

	public Account getConnection();

}
