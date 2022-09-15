package de.tomsplayground.peanuts.domain.comparision;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.base.INamedElement;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.util.Day;

@XStreamAlias("comparison")
public class Comparison implements INamedElement {

	private String name;
	
	private List<Security> securities = new ArrayList<>();

	private Security baseSecurity;

	private Day startDate;

	public List<Security> getSecurities() {
		return securities;
	}

	public void setSecurities(List<Security> securities) {
		this.securities = securities;
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
}
