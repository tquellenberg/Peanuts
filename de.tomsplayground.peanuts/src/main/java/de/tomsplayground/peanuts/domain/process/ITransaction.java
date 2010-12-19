package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;
import java.util.List;

import de.tomsplayground.peanuts.domain.base.Category;

public interface ITransaction extends ITimedElement {

	BigDecimal getAmount();

	Category getCategory();

	String getMemo();

	List<ITransaction> getSplits();

	void setCategory(Category categoryTo);

}
