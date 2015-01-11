package de.tomsplayground.peanuts.domain.process;

import static de.tomsplayground.peanuts.Helper.assertEquals;
import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.util.Day;

public class StopLossTest {

	@Test
	public void testNoTrailingStrategy() {
		Security security = new Security("Test");
		Day start = new Day().addDays(-2);
		BigDecimal startPrice = new BigDecimal("10.00");
		ITrailingStrategy strategy = new NoTrailingStrategy();
		StopLoss stopLoss = new StopLoss(security, start, startPrice, strategy);

		IPriceProvider priceProvider = new PriceProvider() {
			@Override
			public String getName() {
				return "Dummy";
			}
		};
		priceProvider.setPrice(new Price(start, new BigDecimal("13.00")));
		priceProvider.setPrice(new Price(start.addDays(1), new BigDecimal("9.00")));
		priceProvider.setPrice(new Price(start.addDays(2), new BigDecimal("15.00")));

		List<Price> prices = stopLoss.getPrices(priceProvider);

		assertEquals(3, prices.size());
		assertEquals(new BigDecimal("10.00"), prices.get(0).getClose());
		assertEquals(new BigDecimal("10.00"), prices.get(1).getClose());
		assertEquals(new BigDecimal("10.00"), prices.get(2).getClose());
	}

	@Test
	public void testPercentTrailingStrategy() {
		Security security = new Security("Test");
		Day start = new Day().addDays(-2);
		BigDecimal startPrice = new BigDecimal("10.00");
		ITrailingStrategy strategy = new PercentTrailingStrategy(new BigDecimal("0.1"));
		StopLoss stopLoss = new StopLoss(security, start, startPrice, strategy);

		IPriceProvider priceProvider = new PriceProvider() {
			@Override
			public String getName() {
				return "Dummy";
			}
		};
		priceProvider.setPrice(new Price(start, new BigDecimal("13.00")));
		priceProvider.setPrice(new Price(start.addDays(1), new BigDecimal("9.00")));
		priceProvider.setPrice(new Price(start.addDays(2), new BigDecimal("15.00")));

		List<Price> prices = stopLoss.getPrices(priceProvider);
		assertEquals(3, prices.size());
		assertEquals(new BigDecimal("11.70"), prices.get(0).getClose());
		assertEquals(new BigDecimal("11.70"), prices.get(1).getClose());
		assertEquals(new BigDecimal("13.50"), prices.get(2).getClose());
	}


}
