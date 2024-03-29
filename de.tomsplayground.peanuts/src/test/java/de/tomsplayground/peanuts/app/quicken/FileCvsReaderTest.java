package de.tomsplayground.peanuts.app.quicken;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.Month;
import java.util.List;

import org.junit.Test;

import com.opencsv.exceptions.CsvValidationException;

import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.util.Day;


public class FileCvsReaderTest {

	@Test
	public void testRead() throws IOException, CsvValidationException {
		Reader in = new InputStreamReader(FileCsvReader.class.getResourceAsStream("/quotes.TXT"));
		FileCsvReader reader = new FileCsvReader(null, in);
		reader.read();
		in.close();

		List<IPrice> prices = reader.getPrices();
		assertEquals(14, prices.size());
		assertEquals(new BigDecimal("2.392"), prices.get(0).getValue());
		Day date = prices.get(0).getDay();
		assertEquals(Day.of(2007, Month.AUGUST, 1), date);

		Day day = reader.getMinDate();
		assertEquals(Day.of(2007, Month.AUGUST, 1), day);

		day = reader.getMaxDate();
		assertEquals(Day.of(2007, Month.AUGUST, 20), day);
	}
}
