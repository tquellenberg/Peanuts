package de.tomsplayground.peanuts.app.google;

import static org.junit.Assert.*;

import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.Month;

import org.junit.Test;

import de.tomsplayground.peanuts.util.Day;


public class GooglePriceProviderTest {

	@Test
	public void testSimple() throws Exception {
		Reader in = new InputStreamReader(GooglePriceReader.class.getResourceAsStream("/Google_Data.csv"));
		GooglePriceReader googlePriceReader = new GooglePriceReader(null, in);

		assertEquals(Day.of(2011, Month.JANUARY, 28), googlePriceReader.getMaxDate());
		assertEquals(Day.of(2010, Month.FEBRUARY, 1), googlePriceReader.getMinDate());
		assertEquals(new BigDecimal("43.63"), googlePriceReader.getPrice(Day.of(2011, Month.JANUARY, 28)).getValue());
	}
}
