package de.tomsplayground.peanuts.util;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Currency;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tomsplayground.peanuts.domain.process.ITimedElement;
import de.tomsplayground.util.Day;

public class PeanutsUtil {

	private final static Logger log = LoggerFactory.getLogger(PeanutsUtil.class);

	private static final DateFormat dateFormat = DateFormat.getDateInstance();
	private static final DateFormat dateTimeFormat = DateFormat.getDateTimeInstance();
	private static final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
	private static final NumberFormat currencyValueFormat = NumberFormat.getNumberInstance();
	private static final NumberFormat quantityFormat = NumberFormat.getNumberInstance();
	private static final NumberFormat percentFormat = NumberFormat.getPercentInstance();
	private static final NumberFormat percentValueFormat = NumberFormat.getPercentInstance();

	static {
		((DecimalFormat) currencyValueFormat).setMinimumFractionDigits(2);
		((DecimalFormat) currencyValueFormat).setParseBigDecimal(true);
		((DecimalFormat) quantityFormat).setParseBigDecimal(true);
		((DecimalFormat) percentValueFormat).setParseBigDecimal(true);
		percentFormat.setMinimumFractionDigits(2);
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

	public static String formatCurrency(BigDecimal amount, Currency currency) {
		if (amount == null) {
			return "";
		}
		try {
			if (currency != null) {
				synchronized (currencyFormat) {
					currencyFormat.setCurrency(currency);
					return currencyFormat.format(amount);
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
				return percentFormat.format(percent);
			}
		} catch (IllegalArgumentException e) {
			log.error("percent:" + percent);
			throw e;
		}
	}

	public static String formatDate(Day date) {
		if (date == null) {
			return "";
		}
		try {
			synchronized (dateFormat) {
				return dateFormat.format(date.toCalendar().getTime());
			}
		} catch (IllegalArgumentException e) {
			log.error("date:" + date);
			throw e;
		}
	}

	public static String formatDateTime(Date date) {
		synchronized (dateTimeFormat) {
			return dateTimeFormat.format(date);
		}
	}

	public static BigDecimal parseQuantity(String str) throws ParseException {
		BigDecimal amount = (BigDecimal) quantityFormat.parse(str);
		return amount;
	}

	public static BigDecimal parseCurrency(String str) throws ParseException {
		BigDecimal amount = (BigDecimal) currencyValueFormat.parse(str);
		return amount;
	}

	public static BigDecimal parsePercent(String str) throws ParseException {
		BigDecimal amount = (BigDecimal) percentValueFormat.parse(str);
		return amount;
	}

	public static int binarySearch(List<? extends ITimedElement> prices, Day date) {
		int low = 0;
		int high = prices.size() - 1;

		while (low <= high) {
			int mid = (low + high) >> 1;
		ITimedElement midVal = prices.get(mid);
		int cmp = midVal.getDay().compareTo(date);

		if (cmp < 0) {
			low = mid + 1;
		} else if (cmp > 0) {
			high = mid - 1;
		}
		else {
			return mid; // key found
		}
		}
		return -(low +1); // key not found
	}

}
