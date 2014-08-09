package de.tomsplayground.peanuts.domain.fundamental;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.base.InventoryEntry;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.util.Day;

@XStreamAlias("fundamental")
public class FundamentalData {
	
	private int year;
	private BigDecimal dividende;
	private BigDecimal earningsPerShare;
	
	public FundamentalData() {
		this.year = 2000;
		this.dividende = BigDecimal.ZERO;
		this.earningsPerShare = BigDecimal.ZERO;
	}
	
	public FundamentalData(FundamentalData d) {
		this.year = d.year;
		this.dividende = d.dividende;
		this.earningsPerShare = d.earningsPerShare;
	}
	
	public int getYear() {
		return year;
	}
	public void setYear(int year) {
		this.year = year;
	}
	public BigDecimal getDividende() {
		return dividende;
	}
	public void setDividende(BigDecimal dividende) {
		this.dividende = dividende;
	}
	public BigDecimal getEarningsPerShare() {
		return earningsPerShare;
	}
	public void setEarningsPerShare(BigDecimal earningsPerShare) {
		this.earningsPerShare = earningsPerShare;
	}

	public BigDecimal calculatePeRatio(IPriceProvider priceProvider) {
		Price price = priceProvider.getPrice(new Day(year, 11, 31));
		BigDecimal close = price.getClose();
		if (earningsPerShare.signum() == 0) {
			return BigDecimal.ZERO;
		}
		return close.divide(earningsPerShare, new MathContext(10, RoundingMode.HALF_EVEN));
	}

	public BigDecimal calculateDivYield(IPriceProvider priceProvider) {
		Price price = priceProvider.getPrice(new Day(year, 11, 31));
		BigDecimal close = price.getClose();
		if (close.signum() == 0) {
			return BigDecimal.ZERO;
		}
		return dividende.divide(close, new MathContext(10, RoundingMode.HALF_EVEN));
	}
	
	public BigDecimal calculateYOC(InventoryEntry inventoryEntry) {
		if (inventoryEntry.getQuantity().signum() <= 0) {
			return BigDecimal.ZERO;
		}
		if (inventoryEntry.getAvgPrice().signum() == 0) {
			return BigDecimal.ZERO;
		}		
		return dividende.divide(inventoryEntry.getAvgPrice(), new MathContext(10, RoundingMode.HALF_EVEN));
	}
}
