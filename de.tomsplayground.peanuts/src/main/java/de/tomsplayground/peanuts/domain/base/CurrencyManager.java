package de.tomsplayground.peanuts.domain.base;

import java.math.BigDecimal;
import java.util.Currency;

public class CurrencyManager {

	public static final BigDecimal DM_EURO = new BigDecimal("1.95583");

	public BigDecimal getExchangeRate(Currency c1, Currency c2) {
		if (c1.getCurrencyCode().equals("EUR") && c2.getCurrencyCode().equals("DEM")) {
			return DM_EURO;
		}
		if (c1.getCurrencyCode().equals("DEM") && c2.getCurrencyCode().equals("EUR")) {
			return new BigDecimal("0.5112918812");
		}
		return null;
	}

}
