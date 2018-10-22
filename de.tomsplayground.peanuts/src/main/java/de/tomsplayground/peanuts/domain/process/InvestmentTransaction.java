package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.util.Day;

@XStreamAlias("investment-transaction")
public class InvestmentTransaction extends Transaction {

	public enum Type {
		BUY, SELL, INCOME, EXPENSE
	}

	private Security security;
	private BigDecimal price;
	private BigDecimal quantity;
	private BigDecimal commission;
	private Type type;

	/**
	 * Trading (buy and sell) of a security.
	 */
	public InvestmentTransaction(Day day, Security security, BigDecimal price,
		BigDecimal quantity, BigDecimal commission, Type type) {
		super(day, calculateAmount(type, price, quantity, commission));
		if (security == null) {
			throw new IllegalArgumentException("security");
		}
		this.security = security;
		this.price = price;
		this.quantity = quantity;
		this.commission = commission;
		this.type = type;
	}

	public InvestmentTransaction(InvestmentTransaction t) {
		super(t);
		if (t.getSecurity() == null) {
			throw new IllegalArgumentException("security");
		}
		if (t.getType() == null) {
			throw new IllegalArgumentException("type");
		}
		this.security = t.getSecurity();
		this.price = t.getPrice();
		this.quantity = t.getQuantity();
		this.commission = t.getCommission();
		this.type = t.getType();
	}

	public BigDecimal getPrice() {
		return price;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public Security getSecurity() {
		return security;
	}

	public void setSecurity(Security security) {
		Security oldValue = this.security;
		this.security = security;
		firePropertyChange("security", oldValue, security);
	}

	public BigDecimal getCommission() {
		return commission;
	}

	public static BigDecimal calculateAmount(Type type, BigDecimal price, BigDecimal quantity, BigDecimal commission) {
		return (type == Type.SELL || type == Type.INCOME ?
				price.multiply(quantity).setScale(2, RoundingMode.HALF_UP) :
				price.multiply(quantity).setScale(2, RoundingMode.HALF_UP).negate())
			.subtract(commission);
	}

	public void setInvestmentDetails(Type type, BigDecimal price, BigDecimal quantity, BigDecimal commission) {
		this.type = type;
		this.price = price;
		this.quantity = quantity;
		this.commission = commission;
		super.setAmount(calculateAmount(type, price, quantity, commission));
		firePropertyChange("investment", null, null);
	}

	@Override
	public void setAmount(BigDecimal amount) {
		throw new UnsupportedOperationException();
	}

	public Type getType() {
		return type;
	}

	@Override
	public Object clone() {
		return super.clone();
	}

	@Override
	public void reconfigureAfterDeserialization(AccountManager accountManager) {
		super.reconfigureAfterDeserialization(accountManager);

		if (type == Type.INCOME && price == null && quantity == null) {
			quantity = BigDecimal.ONE;
			price = getAmount();
		}
	}
}
