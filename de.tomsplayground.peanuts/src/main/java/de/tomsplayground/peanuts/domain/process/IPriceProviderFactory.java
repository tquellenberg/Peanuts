package de.tomsplayground.peanuts.domain.process;

import de.tomsplayground.peanuts.domain.base.Security;

public interface IPriceProviderFactory {

	IPriceProvider getPriceProvider(Security security);

}