package de.tomsplayground.peanuts.domain.comparision;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.base.INamedElement;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.util.Day;

@XStreamAlias("comparison")
public class Comparison implements INamedElement {

	private String name;
	
	private final List<Security> securities = new ArrayList<>();

	private Security baseSecurity;

	private Day startDate;

	public List<Security> getSecurities() {
		return List.copyOf(securities);
	}

	public void setSecurities(List<Security> securities) {
		this.securities.clear();
		this.securities.addAll(securities);
	}

	public Security getBaseSecurity() {
		return baseSecurity;
	}

	public void setBaseSecurity(Security baseSecurity) {
		this.baseSecurity = baseSecurity;
	}

	public Day getStartDate() {
		return startDate;
	}

	public void setStartDate(Day startDate) {
		this.startDate = startDate;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
				.append("name", name)
				.toString();
	}

}
