package de.tomsplayground.peanuts.client.navigation;

import org.eclipse.core.expressions.PropertyTester;

import de.tomsplayground.peanuts.domain.base.IDeletable;

public class NavigationPropertyTester extends PropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (property.equals("deleted") && receiver instanceof IDeletable deletable) {
			boolean expected = false;
			if (expectedValue instanceof Boolean bValue) {
				expected = bValue.booleanValue();
			}
			return (deletable.isDeleted() == expected);
		}
		return false;
	}

}
