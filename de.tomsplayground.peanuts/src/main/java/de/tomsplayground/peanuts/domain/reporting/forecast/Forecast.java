package de.tomsplayground.peanuts.domain.reporting.forecast;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.base.INamedElement;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.domain.reporting.transaction.Report;
import de.tomsplayground.util.Day;

@XStreamAlias("forecast")
public class Forecast extends ObservableModelObject implements INamedElement {

	private String name;
	private BigDecimal startAmount;
	private BigDecimal annualIncrease;
	private Day startDay;
	private BigDecimal annualPercent;
	private Set<Report> connected = new HashSet<Report>();

	public Forecast(String name) {
		this.name = name;
		this.startAmount = BigDecimal.ZERO;
		this.annualIncrease = BigDecimal.ZERO;
		this.annualPercent = BigDecimal.ZERO;
		this.startDay = new Day();
	}
	
	public Forecast(Day startDay, BigDecimal startAmount, BigDecimal annualIncrease) {
		this(startDay, startAmount, annualIncrease, BigDecimal.ZERO);
	}

	public Forecast(Day startDay, BigDecimal startAmount, BigDecimal annualIncrease, BigDecimal annualPercent) {
		this.startDay = startDay;
		this.startAmount = startAmount;
		this.annualIncrease = annualIncrease;
		this.annualPercent = annualPercent;
	}

	public BigDecimal getValue(Day day) {
		if (day.before(startDay))
			throw new IllegalArgumentException(day + " is before " +startDay);
		int delta = startDay.delta(day);
		BigDecimal result = startAmount; 
		while (delta > 360) {
			result = result.add(result.multiply(annualPercent.movePointLeft(2)));
			result = result.add(annualIncrease);
			delta = delta - 360;
		}
		BigDecimal factor = new BigDecimal(delta).divide(new BigDecimal(360), new MathContext(10, RoundingMode.HALF_EVEN));
		result = result.add(result.multiply(annualPercent.movePointLeft(2)).multiply(factor));
		result = result.add(annualIncrease.multiply(factor));
		return result;
	}

	public BigDecimal getStartAmount() {
		return startAmount;
	}

	public BigDecimal getAnnualIncrease() {
		return annualIncrease;
	}

	public Day getStartDay() {
		return startDay;
	}

	public BigDecimal getAnnualPercent() {
		return annualPercent;
	}

	public void setAnnualPercent(BigDecimal annualPercent) {
		BigDecimal oldAnnualPercent = this.annualPercent;
		this.annualPercent = annualPercent;
		firePropertyChange("annualPercent", oldAnnualPercent, annualPercent);
	}

	public void setStartAmount(BigDecimal startAmount) {
		BigDecimal oldStartAmount = this.startAmount;
		this.startAmount = startAmount;
		firePropertyChange("startAmount", oldStartAmount, startAmount);
	}

	public void setAnnualIncrease(BigDecimal annualIncrease) {
		BigDecimal oldAnnualIncrease = this.annualIncrease;
		this.annualIncrease = annualIncrease;
		firePropertyChange("annualIncrease", oldAnnualIncrease, annualIncrease);
	}

	public void setStartDay(Day startDay) {
		Day oldStartDay = this.startDay;
		this.startDay = startDay;
		firePropertyChange("startDay", oldStartDay, startDay);
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		String oldName = this.name;
		this.name = name;
		firePropertyChange("name", oldName, name);
	}

	public void connect(Report report) {
		connected.add(report);
	}

	public void disconnect(Report report) {
		connected.remove(report);
	}
	
	public boolean isConnected(Report report) {
		return connected.contains(report);
	}

	public void reconfigureAfterDeserialization() {
		if (connected == null)
			connected = new HashSet<Report>();
	}
}
