package de.tomsplayground.peanuts.client.dnd;

import java.io.Serializable;
import java.util.List;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.Security;

public class SecurityTransferData implements IPeanutsTransferData, Serializable {

	private static final long serialVersionUID = 3204122388147804957L;

	private final String isin;

	public SecurityTransferData(Security security) {
		isin = security.getISIN();
		if (isin == null)
			throw new IllegalArgumentException();
	}	
	
	public Security getSecurity() {
		List<Security> securities = Activator.getDefault().getAccountManager().getSecurities();
		for (Security security : securities) {
			if (isin.equals(security.getISIN()))
				return security;
		}
		throw new IllegalStateException("Security not found: " + isin);
	}

}
