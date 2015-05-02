package de.tomsplayground.peanuts.domain.process;

import de.tomsplayground.peanuts.domain.base.Security;


public class EmptyPriceProvider extends PriceProvider {

	public EmptyPriceProvider(Security security) {
		super(security);
	}

	@Override
	public String getName() {
		return "Empty";
	}

}
