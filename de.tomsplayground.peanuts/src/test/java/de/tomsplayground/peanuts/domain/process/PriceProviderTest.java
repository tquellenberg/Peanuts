package de.tomsplayground.peanuts.domain.process;

import static org.junit.Assert.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.tomsplayground.peanuts.util.Day;


public class PriceProviderTest {

	private PriceProvider pp;
	private Day c1;
	private Day dateAfterC1;

	@Before
	public void setUp() {
		pp = new PriceProvider(null);
		c1 = Day.of(2008, 4, 1);
		dateAfterC1 = Day.of(2008, 4, 2);
	}

	@Test
	public void testGetPrice() throws Exception {
		Price price1 = new Price(c1, BigDecimal.ZERO);
		pp.setPrice(price1);

		IPrice price = pp.getPrice(c1);
		assertEquals(price1, price);

		price = pp.getPrice(dateAfterC1);
		assertEquals(price1, price);
	}

	@Test
	public void testGetPrice2() {
		pp.setPrice(new Price(Day.of(2008, 4, 2), BigDecimal.ONE));
		pp.setPrice(new Price(Day.of(2008, 4, 5), BigDecimal.TEN));

		IPrice price = pp.getPrice(Day.of(2008, 4, 3));
		assertEquals(BigDecimal.ONE, price.getValue());
	}

	@Test
	public void testSetPrice() throws Exception {

		pp.setPrice(new Price(c1, BigDecimal.ZERO));
		pp.setPrice(new Price(dateAfterC1, BigDecimal.ZERO));

		assertEquals(c1, pp.getMinDate());
		assertEquals(dateAfterC1, pp.getMaxDate());
	}

	@Test
	public void testSetPrices() {
		final PropertyChangeEvent lastEvent[] = new PropertyChangeEvent[1];
		pp.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				lastEvent[0] = evt;
			}
		});
		pp.setPrice(new Price(c1, BigDecimal.ZERO));

		List<Price> prices = new ArrayList<Price>();
		prices.add(new Price(c1, BigDecimal.ZERO));
		prices.add(new Price(dateAfterC1, BigDecimal.ZERO));
		pp.setPrices(prices, true);

		assertEquals(c1, pp.getMinDate());
		assertEquals(dateAfterC1, pp.getMaxDate());
		assertEquals(2, pp.getPrices().size());

		assertEquals(pp, lastEvent[0].getSource());
		assertEquals("prices", lastEvent[0].getPropertyName());
	}

	@Test
	public void testSetPriceReverse() throws Exception {
		pp.setPrice(new Price(dateAfterC1, BigDecimal.ZERO));
		pp.setPrice(new Price(c1, BigDecimal.ZERO));

		assertEquals(c1, pp.getMinDate());
		assertEquals(dateAfterC1, pp.getMaxDate());
	}

	@Test
	public void addPrice() throws Exception {
		final PropertyChangeEvent lastEvent[] = new PropertyChangeEvent[1];
		pp.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				lastEvent[0] = evt;
			}
		});

		// Add price
		Price price1 = new Price(c1, BigDecimal.ZERO);
		pp.setPrice(price1);

		assertNotNull("Event expected", lastEvent[0]);
		assertEquals(pp, lastEvent[0].getSource());
		assertEquals("prices", lastEvent[0].getPropertyName());
	}

	@Test
	public void removePrice() throws Exception {
		final PropertyChangeEvent lastEvent[] = new PropertyChangeEvent[1];
		pp.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				lastEvent[0] = evt;
			}
		});
		pp.setPrice(new Price(c1, BigDecimal.ZERO));
		pp.setPrice(new Price(c1.addDays(1), BigDecimal.ZERO));
		pp.setPrice(new Price(c1.addDays(2), BigDecimal.ZERO));

		pp.removePrice(c1.addDays(1));

		assertEquals(2, pp.getPrices().size());
		assertNotNull("Event expected", lastEvent[0]);
		assertEquals(pp, lastEvent[0].getSource());
		assertEquals("prices", lastEvent[0].getPropertyName());
	}

	@Test
	public void changeExistingPrice() throws Exception {
		Price price1 = new Price(c1, BigDecimal.ZERO);
		pp.setPrice(price1);
		final PropertyChangeEvent lastEvent[] = new PropertyChangeEvent[1];
		pp.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				lastEvent[0] = evt;
			}
		});

		// Change existing price via priceProvider
		lastEvent[0] = null;
		Price price2 = new Price(c1, new BigDecimal("2"));
		pp.setPrice(price2, true);

		assertNotNull("Event expected", lastEvent[0]);
		assertEquals(pp, lastEvent[0].getSource());
		assertEquals("prices", lastEvent[0].getPropertyName());
		assertEquals(price1, lastEvent[0].getOldValue());
		assertEquals(price2, lastEvent[0].getNewValue());
	}

	@Test
	public void keepExistingPrice() throws Exception {
		Price price1 = new Price(c1, BigDecimal.ZERO);
		pp.setPrice(price1);
		final PropertyChangeEvent lastEvent[] = new PropertyChangeEvent[1];
		pp.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				lastEvent[0] = evt;
			}
		});

		// Replace existing price via priceProvider
		lastEvent[0] = null;
		Price price2 = new Price(c1, new BigDecimal("2"));
		pp.setPrice(price2, false);

		assertNull("No Event expected", lastEvent[0]);
		assertEquals(price1, pp.getPrices(c1, c1).get(0));
	}

}
