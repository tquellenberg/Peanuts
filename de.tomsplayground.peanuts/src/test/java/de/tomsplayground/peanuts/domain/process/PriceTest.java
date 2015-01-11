package de.tomsplayground.peanuts.domain.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;

import org.junit.Test;

import de.tomsplayground.util.Day;

public class PriceTest {

	@Test
	public void testHashCode() {
		Price price1 = new Price(new Day(2008, 2, 29), new BigDecimal("2.00"));
		Price price2 = new Price(new Day(2008, 2, 29), new BigDecimal("2.00"));
		assertEquals(price1.hashCode(), price2.hashCode());

		Price price3 = new Price(new Day(2008, 2, 29), null, new BigDecimal("2.00"), null, null);
		assertEquals(price1.hashCode(), price3.hashCode());

		Price price4 = new Price(new Day(2008, 2, 28), null, new BigDecimal("2.00"), null, null);
		assertTrue(price1.hashCode() != price4.hashCode());

		Price price5 = new Price(new Day(2008, 2, 29), null, new BigDecimal("2.0"), null, null);
		assertTrue(price1.hashCode() != price5.hashCode());
	}

	@Test
	public void testEqualsObject() {
		Price price1 = new Price(new Day(2008, 2, 29), new BigDecimal("2.00"));
		Price price2 = new Price(new Day(2008, 2, 29), new BigDecimal("2.00"));
		assertEquals(price1, price2);

		Price price3 = new Price(new Day(2008, 2, 29), null, new BigDecimal("2.00"), null, null);
		assertEquals(price1, price3);

		Price price4 = new Price(new Day(2008, 2, 28), null, new BigDecimal("2.00"), null, null);
		assertFalse(price1.equals(price4));

		Price price5 = new Price(new Day(2008, 2, 29), null, new BigDecimal("2.0"), null, null);
		assertFalse(price1.equals(price5));
	}

	@Test
	public void highLowNull() throws Exception {
		Price price1 = new Price(new Day(2008, 2, 29), new BigDecimal("1.00"), new BigDecimal("2.00"), null, null);
		assertEquals(new BigDecimal("1.00"), price1.getLow());
		assertEquals(new BigDecimal("2.00"), price1.getHigh());
	}

//	@Test
//	public void highLowAutoAdjust() throws Exception {
//		Price price1 = new Price(new Day(2008, 2, 29), BigDecimal.ONE, new BigDecimal("2.00"), BigDecimal.ZERO, BigDecimal.ZERO);
//		Helper.assertEquals(BigDecimal.ZERO, price1.getLow());
//		Helper.assertEquals(BigDecimal.ONE, price1.getHigh());
//
//		price1 = new Price(new Day(2008, 2, 29), new BigDecimal("0.5"), new BigDecimal("2.00"), BigDecimal.ZERO, BigDecimal.ZERO);
//		Helper.assertEquals(BigDecimal.ZERO, price1.getLow());
//		Helper.assertEquals(BigDecimal.ONE, price1.getHigh());
//
//		price1 = new Price(new Day(2008, 2, 29), new BigDecimal("0.3"), BigDecimal.TEN, new BigDecimal("0.5"), BigDecimal.ZERO);
//		Helper.assertEquals(new BigDecimal("0.3"), price1.getLow());
//	}
//
//	@Test
//	public void highLowAutoAdjust2() throws Exception {
//		Price price1 = new Price(new Day(2008, 2, 29), null, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO);
//		Helper.assertEquals(BigDecimal.ZERO, price1.getLow());
//		Helper.assertEquals(BigDecimal.ONE, price1.getHigh());
//
//		price1 = new Price(new Day(2008, 2, 29), null, new BigDecimal("0.5"), BigDecimal.ZERO, BigDecimal.ZERO);
//		Helper.assertEquals(BigDecimal.ZERO, price1.getLow());
//		Helper.assertEquals(BigDecimal.ONE, price1.getHigh());
//
//		price1 = new Price(new Day(2008, 2, 29), BigDecimal.TEN, new BigDecimal("0.3"), new BigDecimal("0.5"), BigDecimal.ZERO);
//		Helper.assertEquals(new BigDecimal("0.3"), price1.getLow());
//	}

//	@Test
//	public void highLowAutoAdjustConstructor() throws Exception {
//		Price p = new Price(new Day(2008, 9, 27), new BigDecimal("20.00"), new BigDecimal("5.00"), new BigDecimal("10.00"), new BigDecimal("10.00"));
//
//		Helper.assertEquals(new BigDecimal("20"), p.getHigh());
//		Helper.assertEquals(new BigDecimal("5"), p.getLow());
//	}
}
