package de.tomsplayground.peanuts.domain.process;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.google.common.collect.Lists;

import de.tomsplayground.peanuts.Helper;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.util.Day;


public class AdjustedPricesTest {

	@Test
	public void simpleTest() throws Exception {
		List<Price> prices = new ArrayList<Price>();
		prices.add(new Price(Day.of(2008, Month.NOVEMBER, 1), new BigDecimal("10")));
		prices.add(new Price(Day.of(2008, Month.NOVEMBER, 2), new BigDecimal("5")));
		prices.add(new Price(Day.of(2008, Month.NOVEMBER, 3), new BigDecimal("5")));
		PriceProvider priceProvider = new PriceProvider(null);
		priceProvider.setPrices(prices, true);

		List<StockSplit> splits = Collections.singletonList(new StockSplit(new Security("sec"), Day.of(2008, Month.NOVEMBER, 2), 1, 2));

		SplitAdjustedPriceProvider splitAdjustedPrices = new SplitAdjustedPriceProvider(priceProvider, splits);
		List<IPrice> adjustPrices = splitAdjustedPrices.getPrices();

		assertEquals(3, adjustPrices.size());
		Helper.assertEquals(new BigDecimal("5"), adjustPrices.get(0).getValue());
		Helper.assertEquals(new BigDecimal("5"), adjustPrices.get(1).getValue());
		Helper.assertEquals(new BigDecimal("5"), adjustPrices.get(2).getValue());
	}

	@Test
	public void multiSplitTest() throws Exception {
		List<Price> prices = new ArrayList<Price>();
		prices.add(new Price(Day.of(2008, Month.NOVEMBER, 1), new BigDecimal("10")));
		prices.add(new Price(Day.of(2008, Month.NOVEMBER, 2), new BigDecimal("5")));
		prices.add(new Price(Day.of(2008, Month.NOVEMBER, 3), new BigDecimal("5")));
		PriceProvider priceProvider = new PriceProvider(null);
		priceProvider.setPrices(prices, true);

		List<StockSplit> splits = Lists.newArrayList(
				new StockSplit(new Security("sec"), Day.of(2008, Month.NOVEMBER, 2), 1, 2),
				new StockSplit(new Security("sec"), Day.of(2008, Month.NOVEMBER, 3), 1, 2));

		SplitAdjustedPriceProvider splitAdjustedPrices = new SplitAdjustedPriceProvider(priceProvider, splits);
		List<IPrice> adjustPrices = splitAdjustedPrices.getPrices();

		assertEquals(3, adjustPrices.size());
		Helper.assertEquals(new BigDecimal("2.5"), adjustPrices.get(0).getValue());
		Helper.assertEquals(new BigDecimal("2.5"), adjustPrices.get(1).getValue());
		Helper.assertEquals(new BigDecimal("5"), adjustPrices.get(2).getValue());
	}
	
	@Test
	public void changeListenerTest() {
		List<Price> prices = new ArrayList<Price>();
		prices.add(new Price(Day.of(2008, Month.NOVEMBER, 1), new BigDecimal("10")));
		prices.add(new Price(Day.of(2008, Month.NOVEMBER, 2), new BigDecimal("5")));
		prices.add(new Price(Day.of(2008, Month.NOVEMBER, 3), new BigDecimal("5")));
		PriceProvider priceProvider = new PriceProvider(null);
		priceProvider.setPrices(prices, true);
		
		List<StockSplit> splits = Lists.newArrayList(
				new StockSplit(new Security("sec"), Day.of(2008, Month.NOVEMBER, 2), 1, 2),
				new StockSplit(new Security("sec"), Day.of(2008, Month.NOVEMBER, 3), 1, 2));

		SplitAdjustedPriceProvider splitAdjustedPrices = new SplitAdjustedPriceProvider(priceProvider, splits);

		AtomicBoolean check = new AtomicBoolean();
		((ObservableModelObject)splitAdjustedPrices).addPropertyChangeListener(e -> {
			check.set(true);
		});
		
		priceProvider.setPrice(new Price(Day.of(2008, Month.NOVEMBER, 3), new BigDecimal("99")));

		assertTrue(check.get());
	}
}
