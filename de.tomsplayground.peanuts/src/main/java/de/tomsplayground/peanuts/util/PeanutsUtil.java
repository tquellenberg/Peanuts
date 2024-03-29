package de.tomsplayground.peanuts.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.Currency;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tomsplayground.peanuts.calculator.Calculator;
import de.tomsplayground.peanuts.domain.process.ITimedElement;

public class PeanutsUtil {

	private final static Logger log = LoggerFactory.getLogger(PeanutsUtil.class);

	private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
	private static final DateTimeFormatter yearMonthFormatter = DateTimeFormatter.ofPattern("MMM yyyy");

	private static final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
	private static final NumberFormat currencyValueFormat = NumberFormat.getNumberInstance();
	private static final NumberFormat quantityFormat = NumberFormat.getNumberInstance();
	private static final NumberFormat percentFormat = NumberFormat.getPercentInstance();
	private static final NumberFormat percentValueFormat = NumberFormat.getPercentInstance();

	public static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);
	
	public static final Calculator calculator = new Calculator();

	static {
		((DecimalFormat) currencyValueFormat).setMinimumFractionDigits(2);
		((DecimalFormat) currencyValueFormat).setMaximumFractionDigits(4);
		((DecimalFormat) currencyValueFormat).setParseBigDecimal(true);
		((DecimalFormat) quantityFormat).setMaximumFractionDigits(8);
		((DecimalFormat) quantityFormat).setParseBigDecimal(true);
		((DecimalFormat) percentValueFormat).setParseBigDecimal(true);
		percentFormat.setMinimumFractionDigits(2);
		calculator.setMathContext(MC);
	}

	public static String format(BigDecimal amount, int fractionDigits) {
		if (amount == null) {
			return "";
		}
		NumberFormat numberInstance = NumberFormat.getNumberInstance();
		((DecimalFormat) numberInstance).setParseBigDecimal(true);
		((DecimalFormat) numberInstance).setMaximumFractionDigits(fractionDigits);
		((DecimalFormat) numberInstance).setMinimumFractionDigits(fractionDigits);
		try {
			return numberInstance.format(amount);
		} catch (IllegalArgumentException e) {
			log.error("amount:" + amount);
			throw e;
		}
	}

	public static String formatHugeNumber(BigDecimal value) {
		NumberFormat numberInstance = NumberFormat.getNumberInstance();
		numberInstance.setMinimumFractionDigits(1);
		numberInstance.setMaximumFractionDigits(1);
		if (value.compareTo(new BigDecimal(1000 * 1000 * 1000)) > 0) {
			BigDecimal v2 = value.divide(new BigDecimal(1000 * 1000 * 1000), PeanutsUtil.MC);
			return numberInstance.format(v2.setScale(1, RoundingMode.HALF_UP)) + " Mrd";
		}
		if (value.compareTo(new BigDecimal(1000 * 1000)) > 0) {
			BigDecimal v2 = value.divide(new BigDecimal(1000 * 1000), PeanutsUtil.MC);
			return numberInstance.format(v2.setScale(1, RoundingMode.HALF_UP)) + " Mio";
		}
		return numberInstance.format(value);
	}

	public static String formatCurrency(BigDecimal amount, Currency currency) {
		if (amount == null) {
			return "";
		}
		try {
			if (currency != null) {
				synchronized (currencyFormat) {
					currencyFormat.setCurrency(currency);
					return currencyFormat.format(amount.setScale(2, RoundingMode.HALF_UP));
				}
			} else {
				synchronized (currencyValueFormat) {
					return currencyValueFormat.format(amount);
				}
			}
		} catch (IllegalArgumentException e) {
			log.error("amount:" + amount);
			throw e;
		}
	}

	public static String formatQuantity(BigDecimal quantity) {
		if (quantity == null) {
			return "";
		}
		try {
			synchronized (quantityFormat) {
				return quantityFormat.format(quantity);
			}
		} catch (IllegalArgumentException e) {
			log.error("quantity:" + quantity);
			throw e;
		}
	}

	public static String formatPercent(BigDecimal percent) {
		if (percent == null) {
			return "";
		}
		try {
			synchronized (percentFormat) {
				String value = percentFormat.format(percent);
				return StringUtils.replace(value, "\u00a0%", "%");
			}
		} catch (IllegalArgumentException e) {
			log.error("percent:" + percent);
			throw e;
		}
	}

	public static BigDecimal parsePercent(String str) throws ParseException {
		str =  StringUtils.replace(str, "%", "\u00a0%");
		BigDecimal amount = (BigDecimal) percentValueFormat.parse(str);
		return amount;
	}

	public static String formatDate(Day date) {
		if (date == null) {
			return "";
		}
		return date.toLocalDate().format(dateFormatter);
	}

	public static String formatMonth(YearMonth month) {
		return month.format(yearMonthFormatter);
	}

	public static BigDecimal parseQuantity(String str) throws ParseException {
		BigDecimal amount = (BigDecimal) quantityFormat.parse(str);
		return amount;
	}

	public static BigDecimal parseCurrency(String source) throws ParseException {
		source = StringUtils.strip(source);
		BigDecimal amount;
		try {
			ParsePosition pp = new ParsePosition(0);
			amount = (BigDecimal) currencyValueFormat.parse(source, pp);
			if (amount == null || pp.getIndex() < source.length()) {
				throw new ParseException("Unparseable number: \"" + source + "\"", pp.getErrorIndex());
			}
		} catch (ParseException e) {
			try {
				amount = calculator.parse(source);
			} catch (RuntimeException e2) {
				// Original ParseException
				throw e;
			}
		}
		return amount;
	}

	/**
	 *
	 * @param prices
	 * @param date
	 * @return the index of the search key, if it is contained in the list; otherwise, (-(insertion point) - 1).
	 *    The insertion point is defined as the point at which the key would be inserted into the list: the index of
	 *    the first element greater than the key, or list.size() if all elements in the list are less than the specified key.
	 *    Note that this guarantees that the return value will be >= 0 if and only if the key is found.
	 */
	public static int binarySearch(List<? extends ITimedElement> prices, Day date) {
		return Collections.binarySearch(prices, new ITimedElement() {
			@Override
			public Day getDay() {
				return date;
			}
		}, (a, b) -> a.getDay().compareTo(b.getDay()));
	}

}
