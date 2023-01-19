package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.config.ConfigurableSupport;
import de.tomsplayground.peanuts.config.IConfigurable;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

@XStreamAlias("credit")
public class Credit extends ObservableModelObject implements ICredit, IConfigurable {

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
	private Map<String, String> displayConfiguration = new HashMap<String, String>();

	private Account connection;

	public Credit(String name) {
		this(Day.today(), Day.today(), BigDecimal.ZERO, BigDecimal.ZERO);
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
		if (day.before(start)) {
			return BigDecimal.ZERO;
		}
		int month;
		BigDecimal a;
		BigDecimal paymentInterest = BigDecimal.ZERO;
		if (paymentInterval == PaymentInterval.MONTHLY) {
			if (day.year == start.year) {
				a = amount;
				month = day.getMonth().getValue() - start.getMonth().getValue() + 1;
			} else {
				a  = amount(Day.firstDayOfYear(day.year));
				month = day.getMonth().getValue();
			}
			BigDecimal p = BigDecimal.ZERO;
			for (int i = 0; i < month-1; i++) {
				p = p.add(paymentAmount);
				paymentInterest = paymentInterest.add(p.multiply(interestRate).divide(new BigDecimal(1200), PeanutsUtil.MC));
			}
		} else {
			if (day.year == start.year) {
				a = amount;
				month = day.getMonth().getValue() - start.getMonth().getValue() + 1;
			} else {
				a  = amount(Day.firstDayOfYear(day.year));
				month = day.getMonth().getValue();
				int paymentMonth = month - (start.getMonth().getValue() - 1);
				if (paymentMonth > 0) {
					paymentInterest = paymentAmount.multiply(interestRate).multiply(new BigDecimal(paymentMonth)).divide(new BigDecimal(1200), PeanutsUtil.MC);
				}
			}
		}
		// (amount * interest * month / 12 / 100) - paymentInterest
		return a.multiply(interestRate).multiply(new BigDecimal(month)).divide(new BigDecimal(1200), PeanutsUtil.MC).subtract(paymentInterest);
	}

	public void setPayment(BigDecimal payment) {
		BigDecimal oldPaymentAmount = this.paymentAmount;
		this.paymentAmount = payment;
		firePropertyChange("payment", oldPaymentAmount, payment);
	}

	@Override
	public BigDecimal amount(Day day) {
		if (day.before(start) || day.after(end)) {
			return BigDecimal.ZERO;
		}
		BigDecimal a;
		if (paymentInterval == PaymentInterval.MONTHLY) {
			int month;
			if (day.year == start.year) {
				a = amount;
				month = day.getMonth().getValue() - start.getMonth().getValue();
			} else {
				Day endOfLastYear = Day.lastDayOfYear(day.year - 1);
				a = amount(endOfLastYear).add(getInterest(endOfLastYear));
				month = day.getMonth().getValue();
			}
			a = a.subtract(paymentAmount.multiply(new BigDecimal(month)));
		} else {
			if (day.year == start.year) {
				a = amount;
			} else {
				Day endOfLastYear = Day.lastDayOfYear(day.year - 1);
				a = amount(endOfLastYear).add(getInterest(endOfLastYear));
				if (day.getMonth().getValue() > start.getMonth().getValue() 
						|| (day.getMonth() == start.getMonth() && day.day >= start.day)) {
					a = a.subtract(paymentAmount);
				}
			}
		}
		return a;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setConnection(Account account) {
		Account oldConnection = this.connection;
		this.connection = account;
		firePropertyChange("connection", oldConnection, connection);
	}

	@Override
	public Account getConnection() {
		return connection;
	}

	public void setName(String name) {
		String oldName = this.name;
		this.name = name;
		firePropertyChange("name", oldName, name);
	}

	public BigDecimal getInterestRate() {
		return interestRate;
	}

	public void setInterestRate(BigDecimal interestRate) {
		BigDecimal oldInterestRate = this.interestRate;
		this.interestRate = interestRate;
		firePropertyChange("interestRate", oldInterestRate, interestRate);
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		BigDecimal oldAmount = this.amount;
		this.amount = amount;
		firePropertyChange("amount", oldAmount, amount);
	}

	@Override
	public Day getStart() {
		return start;
	}

	public void setStart(Day start) {
		Day oldStart = this.start;
		this.start = start;
		firePropertyChange("start", oldStart, start);
	}

	@Override
	public Day getEnd() {
		return end;
	}

	public void setEnd(Day end) {
		Day oldEnd = this.end;
		this.end = end;
		firePropertyChange("end", oldEnd, end);
	}

	public BigDecimal getPaymentAmount() {
		return paymentAmount;
	}

	public PaymentInterval getPaymentInterval() {
		return paymentInterval;
	}

	public void setPaymentInterval(PaymentInterval paymentInterval) {
		PaymentInterval oldPaymentInterval = this.paymentInterval;
		this.paymentInterval = paymentInterval;
		firePropertyChange("paymentInterval", oldPaymentInterval, paymentInterval);
	}

	public void reconfigureAfterDeserialization() {
		if (displayConfiguration == null) {
			displayConfiguration = new HashMap<String, String>();
		}
	}

	private transient ConfigurableSupport configurableSupport;

	private ConfigurableSupport getConfigurableSupport() {
		if (configurableSupport == null) {
			configurableSupport = new ConfigurableSupport(displayConfiguration, null);
		}
		return configurableSupport;
	}

	@Override
	public String getConfigurationValue(String key) {
		return getConfigurableSupport().getConfigurationValue(key);
	}

	@Override
	public void putConfigurationValue(String key, String value) {
		getConfigurableSupport().putConfigurationValue(key, value);
	}
}
