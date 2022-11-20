package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;
import java.util.List;

import de.tomsplayground.peanuts.domain.process.InvestmentTransaction.Type;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

/**
 * Works for one security.
 *
 */
public class SplitAdjustedTransactionProvider {

	private List<StockSplit> stockSplits;
	private Day inventoryDate;

	public SplitAdjustedTransactionProvider(List<StockSplit> securityStockSplits) {
		this.stockSplits = securityStockSplits;
	}
	
	public InvestmentTransaction adjust(InvestmentTransaction invT) {
		if (stockSplits.isEmpty()) {
			return invT;
		}
		BigDecimal splitRatio = getSplitRatio(invT.getDay());
		if (splitRatio.compareTo(BigDecimal.ONE) != 0 && (invT.getType() == Type.BUY || invT.getType() == Type.SELL)) {
			InvestmentTransaction adjustedTransaction = new InvestmentTransaction(invT);
			BigDecimal quantity = invT.getQuantity().divide(splitRatio, PeanutsUtil.MC);
			BigDecimal price = invT.getPrice().multiply(splitRatio, PeanutsUtil.MC);
			adjustedTransaction.setInvestmentDetails(invT.getType(), price, quantity, invT.getCommission());
			return adjustedTransaction;
		}
		return invT;
	}
	
	private BigDecimal getSplitRatio(Day txDay) {
		return stockSplits.stream()
			.filter(split -> split.getDay().beforeOrEquals(inventoryDate))
			.filter(split -> split.getDay().after(txDay))
			.map(split -> split.getRatio())
			.reduce(BigDecimal.ONE, BigDecimal::multiply);
	}

	public void setDate(Day day) {
		this.inventoryDate = day;
	}

}
