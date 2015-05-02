package de.tomsplayground.peanuts.client.navigation;

import org.eclipse.core.expressions.PropertyTester;

import de.tomsplayground.peanuts.domain.base.IDeletable;

public class NavigationPropertyTester extends PropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (property.equals("deleted") && receiver instanceof IDeletable) {
			boolean expected = false;
			if (expectedValue instanceof Boolean) {
				expected = ((Boolean)expectedValue).booleanValue();
			}
			return (((IDeletable)receiver).isDeleted() == expected);
		}
		return false;
	}

}
