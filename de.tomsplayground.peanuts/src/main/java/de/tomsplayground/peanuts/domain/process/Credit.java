package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.config.IConfigurable;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.util.Day;

@XStreamAlias("credit")
public class Credit implements ICredit, IConfigurable {
	
	public enum PaymentInterval {
		MONTHLY,
		YEARLY
	}
	
	private String name;
	private BigDecimal interestRate;
	private BigDecimal amount;
	private Day start;
	private Day end;
	private BigDecimal paymentAmount;
	private PaymentInterval paymentInterval;
	private Map<String, String> displayConfiguration = new ConcurrentHashMap<String, String>();

	private Account connection;
	
	public Credit(String name) {
		this(new Day(), new Day(), BigDecimal.ZERO, BigDecimal.ZERO);
		this.name = name;
	}

	public Credit(Day start, Day end, BigDecimal amount, BigDecimal interestRate) {
		this.start = start;
		this.end = end;
		this.amount = amount;
		this.interestRate = interestRate;
		this.paymentAmount = BigDecimal.ZERO;
		this.paymentInterval = PaymentInterval.MONTHLY;
	}

	@Override
	public BigDecimal getInterest(Day day) {
		if (day.before(start))
			return BigDecimal.ZERO;
		int month;
		BigDecimal a;
		BigDecimal paymentInterest = BigDecimal.ZERO;
		if (paymentInterval == PaymentInterval.MONTHLY) {
			if (day.getYear() == start.getYear()) {
				a = amount;
				month = day.getMonth() - start.getMonth() + 1;
			} else {
				a  = amount(new Day(day.getYear(), 0, 1));
				month = day.getMonth() + 1;
			}
			BigDecimal p = BigDecimal.ZERO;
			for (int i = 0; i < month-1; i++) {
				p = p.add(paymentAmount);
				paymentInterest = paymentInterest.add(p.multiply(interestRate).divide(new BigDecimal(1200), new MathContext(10, RoundingMode.HALF_EVEN)));
			}
		} else {
			if (day.getYear() == start.getYear()) {
				a = amount;
				month = day.getMonth() - start.getMonth() + 1;
			} else {
				a  = amount(new Day(day.getYear(), 0, 1));
				month = day.getMonth() + 1;
				int paymentMonth = month - start.getMonth();
				if (paymentMonth > 0) {
					paymentInterest = paymentAmount.multiply(interestRate).multiply(new BigDecimal(paymentMonth)).divide(new BigDecimal(1200), new MathContext(10, RoundingMode.HALF_EVEN));
				}
			}
		}
		// (amount * interest * month / 12 / 100) - paymentInterest
		return a.multiply(interestRate).multiply(new BigDecimal(month)).divide(new BigDecimal(1200), new MathContext(10, RoundingMode.HALF_EVEN)).subtract(paymentInterest);
	}

	public void setPayment(BigDecimal payment) {
		this.paymentAmount = payment;
	}

	@Override
	public BigDecimal amount(Day day) {
		if (day.before(start) || day.after(end))
			return BigDecimal.ZERO;
		BigDecimal a;
		if (paymentInterval == PaymentInterval.MONTHLY) {
			int month;
			if (day.getYear() == start.getYear()) {
				a = amount;
				month = day.getMonth() - start.getMonth();
			} else {
				Day endOfLastYear = new Day(day.getYear() - 1, 11, 31);
				a = amount(endOfLastYear).add(getInterest(endOfLastYear));
				month = day.getMonth() + 1;
			}
			a = a.subtract(paymentAmount.multiply(new BigDecimal(month)));
		} else {
			if (day.getYear() == start.getYear()) {
				a = amount;
			} else {
				Day endOfLastYear = new Day(day.getYear() - 1, 11, 31);
				a = amount(endOfLastYear).add(getInterest(endOfLastYear));
				if (day.getMonth() > start.getMonth() || (day.getMonth() == start.getMonth() && day.getDay() >= start.getDay()))
					a = a.subtract(paymentAmount);
			}
		}
		return a;
	}

	@Override
	public String getName() {
		return name;
	}
	
	public void setConnection(Account account) {
		connection = account;
	}

	@Override
	public Account getConnection() {
		return connection;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BigDecimal getInterestRate() {
		return interestRate;
	}

	public void setInterestRate(BigDecimal interestRate) {
		this.interestRate = interestRate;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	@Override
	public Day getStart() {
		return start;
	}

	public void setStart(Day start) {
		this.start = start;
	}

	@Override
	public Day getEnd() {
		return end;
	}

	public void setEnd(Day end) {
		this.end = end;
	}

	public BigDecimal getPaymentAmount() {
		return paymentAmount;
	}

	public PaymentInterval getPaymentInterval() {
		return paymentInterval;
	}

	public void setPaymentInterval(PaymentInterval paymentInterval) {
		this.paymentInterval = paymentInterval;
	}

	public void reconfigureAfterDeserialization() {
		if (displayConfiguration == null)
			displayConfiguration = new HashMap<String, String>();
	}

	@Override
	public Map<String, String> getDisplayConfiguration() {
		return displayConfiguration;
	}
}
