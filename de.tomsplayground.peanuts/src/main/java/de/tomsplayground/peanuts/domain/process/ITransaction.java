package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.base.Category;

public interface ITransaction extends ITimedElement {

	BigDecimal getAmount();

	Category getCategory();

	String getMemo();

	ImmutableList<ITransaction> getSplits();

	void setCategory(Category categoryTo);

}
