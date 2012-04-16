package de.tomsplayground.util;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.text.ParseException;

import org.junit.Test;

import de.tomsplayground.peanuts.Helper;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class PeanutsUtilTest {

	@Test
	public void testFormatPercent() {
		assertEquals("80,00%", PeanutsUtil.formatPercent(new BigDecimal("0.8")));
	}

	@Test
	public void testparsePercent() throws ParseException {
		Helper.assertEquals(new BigDecimal("0.8"), PeanutsUtil.parsePercent("80,00%"));
	}

}