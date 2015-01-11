package de.tomsplayground.peanuts.domain.process;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.Test;

import de.tomsplayground.peanuts.domain.base.Category;
import de.tomsplayground.util.Day;


public class EuroTransactionWrapperTest {

	@Test
	public void simple() throws Exception {
		Day day = new Day(2000, 5, 10);
		Category cat = new Category("test", Category.Type.EXPENSE);
		Transaction t = new Transaction(day, BigDecimal.TEN, cat, "memo");
		EuroTransactionWrapper wrapper = new EuroTransactionWrapper(t, Currency.getInstance("DEM"));

		assertEquals(day, wrapper.getDay());
		assertEquals(cat, wrapper.getCategory());
		assertEquals("memo", wrapper.getMemo());
	}

	@Test
	public void splitTransaction() throws Exception {
		Day day = new Day(2000, 5, 10);
		Transaction t = new Transaction(day, BigDecimal.ZERO);
		t.addSplit(new Transaction(day, BigDecimal.TEN));
		t.addSplit(new Transaction(day, BigDecimal.TEN));
		EuroTransactionWrapper wrapper = new EuroTransactionWrapper(t, Currency.getInstance("DEM"));

		assertEquals(2, wrapper.getSplits().size());
		assertEquals(EuroTransactionWrapper.class, wrapper.getSplits().get(0).getClass());
	}
}
