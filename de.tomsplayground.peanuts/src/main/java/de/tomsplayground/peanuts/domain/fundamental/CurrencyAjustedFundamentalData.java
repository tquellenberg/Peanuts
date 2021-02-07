package de.tomsplayground.peanuts.domain.fundamental;

import java.math.BigDecimal;
import java.util.Currency;

import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;
import de.tomsplayground.peanuts.domain.process.CurrencyAdjustedPriceProvider;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.PriceProviderUtils;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class CurrencyAjustedFundamentalData extends FundamentalData {

	private final CurrencyConverter currencyConverter;

	public CurrencyAjustedFundamentalData(FundamentalData fundamentalData, CurrencyConverter currencyConverter) {
		super(fundamentalData);
		this.currencyConverter = currencyConverter;
		if (currencyConverter != null && ! fundamentalData.getCurrency().equals(currencyConverter.getFromCurrency())) {
			throw new IllegalArgumentException("Fundamental data and currency converter must use same currency. ("
				+fundamentalData.getCurrency()+", "+currencyConverter.getFromCurrency()+")");
		}
	}

	@Override
	public BigDecimal getDividende() {
		return currencyConverter.convert(super.getDividende(), getFiscalEndDay());
	}

	@Override
	public BigDecimal getEarningsPerShare() {
		return currencyConverter.convert(super.getEarningsPerShare(), getFiscalEndDay());
	}

	@Override
	public Currency getCurrency() {
		return currencyConverter.getToCurrency();
	}

	/**
	 * Price earning ratio.
	 * Use earnings in original currency and converts prices to original currency.
	 */
	public BigDecimal calculatePeRatio(IPriceProvider priceProvider) {
		if (! getCurrency().equals(priceProvider.getCurrency())) {
			throw new IllegalArgumentException("Fundamental data and price provider must use same currency. ("+getCurrency()+", "+priceProvider.getCurrency()+")");
		}
		priceProvider = new CurrencyAdjustedPriceProvider(priceProvider, currencyConverter.getInvertedCurrencyConverter());
		BigDecimal avgPrice = PriceProviderUtils.avg(priceProvider, getFiscalStartDay(), getFiscalEndDay());
		BigDecimal eps = super.getEarningsPerShare();
		if (eps.signum() == 0) {
			return BigDecimal.ZERO;
		}
		return avgPrice.divide(eps, PeanutsUtil.MC);
	}

}

