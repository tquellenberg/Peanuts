package de.tomsplayground.peanuts.app.yahoo;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import de.tomsplayground.peanuts.app.yahoo.YahooPriceReader.Type;
import de.tomsplayground.peanuts.domain.base.Security;
import junit.framework.TestCase;

public class YahooPriceReaderTest extends TestCase {

	private Security security;

	@Override
	@Before
	public void setUp() {
		security = new Security("Capgemini");
		security.setTicker("CAP.PA");
	}

//	@Test
//	public void testYahooCurrent() throws IOException {
//		YahooPriceReader forTicker = YahooPriceReader.forTicker(security, Type.CURRENT);
//		assertTrue(forTicker.getPrices().size() == 1);
//	}

	@Test
	public void testYahooLastDays() throws IOException {
		YahooPriceReader forTicker = YahooPriceReader.forTicker(security, Type.LAST_DAYS);
		assertTrue(forTicker.getPrices().size() >= 2);
	}

	@Test
	public void testYahooHistorical() throws IOException {
		YahooPriceReader forTicker = YahooPriceReader.forTicker(security, Type.HISTORICAL);
		assertTrue(forTicker.getPrices().size() > 200);
	}

}
