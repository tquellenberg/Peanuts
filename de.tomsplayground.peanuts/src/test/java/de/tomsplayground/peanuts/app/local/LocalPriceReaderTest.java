package de.tomsplayground.peanuts.app.local;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Month;
import java.util.List;

import org.junit.Test;

import com.opencsv.exceptions.CsvValidationException;

import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.util.Day;
import junit.framework.TestCase;

public class LocalPriceReaderTest extends TestCase {

	@Test
	public void testRead() throws IOException, CsvValidationException {
		Reader in = new InputStreamReader(LocalPriceReader.class.getResourceAsStream("/Yahoo.csv"));
		LocalPriceReader reader = new LocalPriceReader(null, in);
		in.close();

		List<IPrice> prices = reader.getPrices();
		assertEquals(3, prices.size());
		IPrice price = prices.get(0);
		assertEquals(new BigDecimal("52.48"), price.getValue());
		Day date = price.getDay();
		assertEquals(Day.of(2007, Month.MAY, 23), date);
	}

	@Test
	public void testEmpty() throws IOException, CsvValidationException {
		Reader in = new StringReader("");
		LocalPriceReader reader = new LocalPriceReader(null, in);

		List<IPrice> prices = reader.getPrices();
		assertEquals(0, prices.size());
	}

}
