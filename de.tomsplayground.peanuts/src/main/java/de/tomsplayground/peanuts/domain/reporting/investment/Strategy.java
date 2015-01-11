package de.tomsplayground.peanuts.domain.reporting.investment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;

public abstract class Strategy {

	protected final List<Buy> buyList = new ArrayList<Buy>();

	public static final class Buy {
		private BigDecimal remainingQuantity;
		private BigDecimal remainingAmount;
		private final BigDecimal avgPrice;

		Buy(InvestmentTransaction transaction) {
			this.remainingQuantity = transaction.getQuantity();
			this.remainingAmount = transaction.getAmount().negate();
			this.avgPrice = transaction.getAmount().divide(remainingQuantity, RoundingMode.HALF_EVEN).negate();
		}

		BigDecimal getRemainingQuantity() {
			return remainingQuantity;
		}

		public BigDecimal getRemainingAmount() {
			return remainingAmount;
		}

		BigDecimal getAvgPrice() {
			return avgPrice;
		}

		void reduceQuantity(BigDecimal q) {
			if (q.compareTo(remainingQuantity) > 0) {
				throw new IllegalArgumentException();
			}
			if (q.compareTo(remainingQuantity) == 0) {
				remainingQuantity = BigDecimal.ZERO;
				remainingAmount = BigDecimal.ZERO;
			} else {
				remainingQuantity = remainingQuantity.subtract(q);
				remainingAmount = remainingAmount.subtract(q.multiply(avgPrice));
			}
		}
	}

	protected BigDecimal getInvestedAmount() {
		BigDecimal investValue = BigDecimal.ZERO;
		for (Buy buy : buyList) {
			investValue = investValue.add(buy.getRemainingAmount());
		}
		return investValue;
	}

	protected BigDecimal getQuantity() {
		BigDecimal quantity = BigDecimal.ZERO;
		for (Buy buy : buyList) {
			quantity = quantity.add(buy.getRemainingQuantity());
		}
		return quantity;
	}

	abstract void buildBuyList(InvestmentTransaction t);
}