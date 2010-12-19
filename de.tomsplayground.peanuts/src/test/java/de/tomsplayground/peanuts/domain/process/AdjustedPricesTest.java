package de.tomsplayground.peanuts.domain.process;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.tomsplayground.peanuts.Helper;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.util.Day;


public class AdjustedPricesTest {

	@Test
	public void simpleTest() throws Exception {
		List<Price> prices = new ArrayList<Price>();
		prices.add(new Price(new Day(2008, 10, 1), new BigDecimal("10")));
		prices.add(new Price(new Day(2008, 10, 2), new BigDecimal("5")));
		prices.add(new Price(new Day(2008, 10, 3), new BigDecimal("5")));
		StockSplit split = new StockSplit(new Security("sec"), new Day(2008, 10, 2), 1, 2);
		
		AdjustedPrices adjustedPrices = new AdjustedPrices();
		List<IPrice> adjustPrices = adjustedPrices.adjustPrices(prices, split);
		
		assertEquals(3, adjustPrices.size());
		Helper.assertEquals(new BigDecimal("5"), adjustPrices.get(0).getValue());
		Helper.assertEquals(new BigDecimal("5"), adjustPrices.get(1).getValue());
		Helper.assertEquals(new BigDecimal("5"), adjustPrices.get(2).getValue());
	}
}
