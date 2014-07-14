package de.tomsplayground.peanuts.domain.fundamental;

import java.math.BigDecimal;

import com.thoughtworks.xstream.annotations.XStreamAlias;

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

}
