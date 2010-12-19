package de.tomsplayground.peanuts.domain.reporting.investment;

import java.math.BigDecimal;

import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;

public class Fifo extends Strategy {

	@Override
	public void buildBuyList(InvestmentTransaction t) {
		if (t.getType() == InvestmentTransaction.Type.BUY) {
			Buy buy = new Buy(t);
			buyList.add(buy);
		} else if (t.getType() == InvestmentTransaction.Type.SELL) {
			BigDecimal quantity = t.getQuantity();
			for (Buy buy : buyList) {
				if (quantity.compareTo(buy.getRemainingQuantity()) > 0) {
					quantity = quantity.subtract(buy.getRemainingQuantity());
					buy.reduceQuantity(buy.getRemainingQuantity());
				} else {
					buy.reduceQuantity(quantity);
					break;
				}
			}
		}
	}
}
