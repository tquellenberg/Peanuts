package de.tomsplayground.peanuts.app.google;

import static org.junit.Assert.*;

import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;

import org.junit.Test;

import de.tomsplayground.peanuts.app.quicken.QifReaderTest;
import de.tomsplayground.util.Day;


public class GooglePriceProviderTest {

	@Test
	public void testSimple() throws Exception {
		Reader in = new InputStreamReader(QifReaderTest.class.getResourceAsStream("/Google_Data.csv"));
		GooglePriceReader googlePriceReader = new GooglePriceReader(null, in);

		assertEquals(new Day(2011, 0, 28), googlePriceReader.getMaxDate());
		assertEquals(new Day(2010, 1, 1), googlePriceReader.getMinDate());
		assertEquals(new BigDecimal("44.25"), googlePriceReader.getPrice(new Day(2011, 0, 28)).getHigh());
	}
}
