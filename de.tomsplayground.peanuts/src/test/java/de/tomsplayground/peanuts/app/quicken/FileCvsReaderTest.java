package de.tomsplayground.peanuts.app.quicken;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.util.Day;


public class FileCvsReaderTest {

	@Test
	public void testRead() throws IOException, ParseException {
		Reader in = new InputStreamReader(QifReaderTest.class.getResourceAsStream("/quotes.TXT"));
		FileCsvReader reader = new FileCsvReader(null, in);
		reader.read();
		IOUtils.closeQuietly(in);

		List<IPrice> prices = reader.getPrices();
		assertEquals(14, prices.size());
		assertEquals(new BigDecimal("2.392"), prices.get(0).getValue());
		Day date = prices.get(0).getDay();
		assertEquals(new Day(2007, 7, 1), date);

		Day day = reader.getMinDate();
		assertEquals(new Day(2007, 7, 1), day);

		day = reader.getMaxDate();
		assertEquals(new Day(2007, 7, 20), day);
	}
}
