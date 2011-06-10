package de.tomsplayground.peanuts.app.csv;

import static org.junit.Assert.*;

import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;

import org.junit.Test;

import de.tomsplayground.peanuts.app.quicken.QifReaderTest;
import de.tomsplayground.util.Day;


public class GoogleCsvPriceProviderTest {

	@Test
	public void testSimple() throws Exception {
		Reader in = new InputStreamReader(QifReaderTest.class.getResourceAsStream("/Google_Data.csv"));
		GoogleCsvPriceProvider googleCsvPriceProvider = new GoogleCsvPriceProvider(in);

		assertEquals(new Day(2011, 0, 28), googleCsvPriceProvider.getMaxDate());
		assertEquals(new Day(2010, 1, 1), googleCsvPriceProvider.getMinDate());
		assertEquals(new BigDecimal("44.25"), googleCsvPriceProvider.getPrice(new Day(2011, 0, 28)).getHigh());
	}
}
