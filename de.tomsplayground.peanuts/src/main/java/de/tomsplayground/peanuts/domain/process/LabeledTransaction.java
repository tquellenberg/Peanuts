package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;

import org.apache.commons.lang3.StringUtils;

import de.tomsplayground.peanuts.util.Day;

public class LabeledTransaction extends Transaction {

	private String label;

	public LabeledTransaction(Day day, BigDecimal amount, String label) {
		super(day, amount);
		if (label == null) {
			throw new IllegalArgumentException("label");
		}
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		label = StringUtils.trimToEmpty(label);
		String oldValue = this.label;
		this.label = label;
		firePropertyChange("label", oldValue, label);
	}

}
