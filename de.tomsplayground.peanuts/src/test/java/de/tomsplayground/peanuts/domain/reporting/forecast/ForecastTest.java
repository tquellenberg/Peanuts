package de.tomsplayground.peanuts.domain.reporting.forecast;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;

import de.tomsplayground.peanuts.Helper;
import de.tomsplayground.util.Day;


public class ForecastTest {

	@Test
	public void simple() throws Exception {
		BigDecimal amount = BigDecimal.TEN;
		BigDecimal increase = BigDecimal.TEN;
		Day startDay = new Day(2008, 7, 20);
		Forecast forecast = new Forecast(startDay, amount, increase);
		
		// Now
		Helper.assertEquals(BigDecimal.TEN, forecast.getValue(startDay));
		// 6 month later
		Helper.assertEquals(new BigDecimal("15"), forecast.getValue(startDay.addMonth(6)));
		// 1 year later
		Helper.assertEquals(new BigDecimal("20"), forecast.getValue(startDay.addYear(1)));
		// 2 year later
		Helper.assertEquals(new BigDecimal("30"), forecast.getValue(startDay.addYear(2)));
	}
	
	@Test
	public void beforeStart() {
		BigDecimal amount = BigDecimal.TEN;
		BigDecimal increase = BigDecimal.TEN;
		Day startDay = new Day(2008, 7, 20);
		Forecast forecast = new Forecast(startDay, amount, increase);

		try {
			forecast.getValue(startDay.addDays(-1));
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// Okay
		}
	}
	
	@Test
	public void percent() throws Exception {
		BigDecimal amount = BigDecimal.TEN;
		BigDecimal increase = BigDecimal.TEN;
		BigDecimal percent = BigDecimal.TEN;
		Day startDay = new Day(2008, 7, 20);
		Forecast forecast = new Forecast(startDay, amount, increase, percent);

		// Now
		Helper.assertEquals(BigDecimal.TEN, forecast.getValue(startDay));
		// 6 month later
		Helper.assertEquals(new BigDecimal("15.50"), forecast.getValue(startDay.addMonth(6)));
		// 1 year later
		Helper.assertEquals(new BigDecimal("21"), forecast.getValue(startDay.addYear(1)));
		// 2 year later
		Helper.assertEquals(new BigDecimal("33.1"), forecast.getValue(startDay.addYear(2)));
	}
}
