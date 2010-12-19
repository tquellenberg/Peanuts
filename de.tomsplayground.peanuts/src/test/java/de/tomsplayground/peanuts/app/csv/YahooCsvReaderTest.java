package de.tomsplayground.peanuts.app.csv;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import de.tomsplayground.peanuts.app.quicken.QifReaderTest;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.util.Day;

public class YahooCsvReaderTest extends TestCase {

	@Test
	public void testRead() throws IOException, ParseException {
		Reader in = new InputStreamReader(QifReaderTest.class.getResourceAsStream("/Yahoo.csv"));
		YahooCsvReader reader = new YahooCsvReader(in);
		reader.read();
		IOUtils.closeQuietly(in);

		List<Price> prices = reader.getPrices();
		assertEquals(3, prices.size());
		Price price = prices.get(0);
		assertEquals(new BigDecimal("52.48"), price.getValue());
		assertEquals(new BigDecimal("53.59"), price.getOpen());
		assertEquals(new BigDecimal("53.60"), price.getHigh());
		assertEquals(new BigDecimal("52.42"), price.getLow());
		assertEquals(new BigDecimal("52.48"), price.getClose());
		Day date = price.getDay();
		assertEquals(new Day(2007, 4, 23), date);
	}
	
	@Test
	public void testEmpty() throws IOException, ParseException {
		Reader in = new StringReader("");
		YahooCsvReader reader = new YahooCsvReader(in, YahooCsvReader.Type.CURRENT);
		reader.read();
		
		List<Price> prices = reader.getPrices();
		assertEquals(0, prices.size());
	}
	
	@Test
	public void testReadCurrent() throws IOException, ParseException {
		Reader in = new InputStreamReader(QifReaderTest.class.getResourceAsStream("/Yahoo_current.csv"));
		YahooCsvReader reader = new YahooCsvReader(in, YahooCsvReader.Type.CURRENT);
		reader.read();
		IOUtils.closeQuietly(in);

		List<Price> prices = reader.getPrices();
		assertEquals(1, prices.size());
		assertEquals(new BigDecimal("129.24"), prices.get(0).getValue());
		Day date = prices.get(0).getDay();
		assertEquals(new Day(2008, 0, 4), date);
	}
	
	@Test
	public void testReadCurrent2() throws IOException, ParseException {
		Reader in = new InputStreamReader(QifReaderTest.class.getResourceAsStream("/Yahoo_current2.csv"));
		YahooCsvReader reader = new YahooCsvReader(in, YahooCsvReader.Type.CURRENT);
		reader.read();
		IOUtils.closeQuietly(in);

		List<Price> prices = reader.getPrices();
		assertEquals(1, prices.size());
		assertEquals(new BigDecimal("5.61"), prices.get(0).getValue());
		assertEquals(new BigDecimal("5.65"), prices.get(0).getOpen());
		assertEquals(new BigDecimal("5.66"), prices.get(0).getHigh());
		assertEquals(new BigDecimal("5.56"), prices.get(0).getLow());
		assertEquals(new BigDecimal("5.61"), prices.get(0).getClose());
		Day date = prices.get(0).getDay();
		assertEquals(new Day(2008, 0, 7), date);
	}

}
