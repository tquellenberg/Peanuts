package de.tomsplayground.peanuts.app.morningstar;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class SectorIndustry {

	public static final SectorIndustry UNKNOWN = new SectorIndustry("", "");

	private final String sector;
	private final String industry;

	public SectorIndustry(String sector, String industry) {
		this.sector = sector;
		this.industry = industry;
	}

	public String getIndustry() {
		return industry;
	}

	public String getSector() {
		return sector;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
