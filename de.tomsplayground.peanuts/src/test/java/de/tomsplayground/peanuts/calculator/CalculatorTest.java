package de.tomsplayground.peanuts.calculator;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

import org.junit.Test;


public class CalculatorTest {

	@Test
	public void testSimple() throws Exception {
		Calculator calculator = new Calculator();

		BigDecimal result = calculator.parse("1+2");
		assertEquals(0, result.compareTo(new BigDecimal("3")));

		result = calculator.parse("2*3");
		assertEquals(0, result.compareTo(new BigDecimal("6")));

		result = calculator.parse("2/4");
		assertEquals(0, result.compareTo(new BigDecimal("0.5")));

		result = calculator.parse("-2");
		assertEquals(0, result.compareTo(new BigDecimal("-2")));

		result = calculator.parse("1+2*3");
		assertEquals(0, result.compareTo(new BigDecimal("7")));

		result = calculator.parse("(1+2)*3");
		assertEquals(0, result.compareTo(new BigDecimal("9")));

		result = calculator.parse("( 1 + 2 ) *\t 3   ");
		assertEquals(0, result.compareTo(new BigDecimal("9")));
	}

	@Test
	public void testMathContext() {
		Calculator calculator = new Calculator();

		BigDecimal result = calculator.parse("1/3");
		assertEquals(0, result.compareTo(new BigDecimal("0.3333333333333333333333333333333333")));

		calculator.setMathContext(new MathContext(10, RoundingMode.HALF_UP));
		result = calculator.parse("1/3");
		assertEquals(0, result.compareTo(new BigDecimal("0.3333333333")));
	}

	@Test
	public void testLanguage() {
		Calculator calculator = new Calculator();
		calculator.setNumberFormat(NumberFormat.getNumberInstance(Locale.GERMANY));
		BigDecimal result = calculator.parse("1,123");
		assertEquals(0, result.compareTo(new BigDecimal("1.123")));

		calculator.setNumberFormat(NumberFormat.getNumberInstance(Locale.US));
		result = calculator.parse("1.123");
		assertEquals(0, result.compareTo(new BigDecimal("1.123")));
	}

	@Test
	public void testInvalidExpression() {
		Calculator calculator = new Calculator();
		try {
			calculator.parse("1++2");
			fail();
		} catch (RuntimeException e) {
			// Okay
		}
	}
}
