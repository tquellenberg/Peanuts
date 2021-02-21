package de.tomsplayground.peanuts.domain.currency;

import static de.tomsplayground.peanuts.Helper.assertEquals;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.Before;
import org.junit.Test;

import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.process.PriceProvider;
import de.tomsplayground.peanuts.util.Day;

public class CurrencyConverterTest {

	private static final Currency EURO = Currency.getInstance("EUR");
	private static final Currency DOLLAR = Currency.getInstance("USD");

	private CurrencyConverter currencyConverter;
	private IPriceProvider priceProvider;

	@Before
	public void setup() {
		priceProvider = new PriceProvider(null);
		currencyConverter = new CurrencyConverter(priceProvider, EURO, DOLLAR);
	}

	@Test
	public void simpleTest() {
		Day date = Day.today();
		priceProvider.setPrice(new Price(date, new BigDecimal("1.20")));
		BigDecimal convert = currencyConverter.convert(new BigDecimal("1.00"), date);

		assertEquals(new BigDecimal("1.20"), convert);
	}

	@Test
	public void simpleReverseTest() {
		Day date = Day.today();
		priceProvider.setPrice(new Price(date, new BigDecimal("1.20")));
		CurrencyConverter invertedCurrencyConverter = currencyConverter.getInvertedCurrencyConverter();
		BigDecimal convert = invertedCurrencyConverter.convert(new BigDecimal("1.20"), date);

		assertEquals(new BigDecimal("1.00"), convert);
		assertEquals(currencyConverter.getFromCurrency(), invertedCurrencyConverter.getToCurrency());
		assertEquals(currencyConverter.getToCurrency(), invertedCurrencyConverter.getFromCurrency());
	}

	@Test
	public void emptyPriceProviderTest() {
		Day date = Day.today();
		try {
			currencyConverter.convert(new BigDecimal("1.20"), date);
			fail();
		} catch (RuntimeException e) {
			// Okay
		}
	}

	@Test
	public void sameCurrenciesTest() {
		currencyConverter = new CurrencyConverter(priceProvider, EURO, EURO);
		Day date = Day.today();
		BigDecimal convert = currencyConverter.convert(new BigDecimal("1.20"), date);

		assertEquals(new BigDecimal("1.20"), convert);
	}

}
