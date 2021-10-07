package de.tomsplayground.peanuts.domain.process;

import static com.google.common.base.Preconditions.*;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.base.IDeletable;
import de.tomsplayground.peanuts.domain.base.INamedElement;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.util.Day;

@XStreamAlias("saved-transcation")
public class SavedTransaction extends ObservableModelObject implements INamedElement, IDeletable {

	private final String name;
	private final Transaction transaction;

	public enum Interval {
		MONTHLY,
		QUARTERLY,
		HALF_YEARLY,
		YEARLY
	}

	private Day start;
	private final Interval interval;

	private boolean deleted;

	public SavedTransaction(String name, Transaction transaction) {
		this(name, transaction, null, null);
	}

	public SavedTransaction(String name, Transaction transaction, Day start, Interval interval) {
		if (name == null) {
			throw new IllegalArgumentException("name");
		}
		if (transaction == null) {
			throw new IllegalArgumentException("transaction");
		}
		this.name = name;
		this.transaction = transaction;
		this.start = start;
		this.interval = Objects.requireNonNullElse(interval, Interval.MONTHLY);
	}

	public Day nextExecution() {
		checkNotNull(start);
		checkNotNull(interval);
		switch (interval) {
			case MONTHLY: return start.addMonth(1);
			case QUARTERLY: return start.addMonth(3);
			case HALF_YEARLY: return start.addMonth(6);
			case YEARLY: return start.addYear(1);
			default: return start.addMonth(1);
		}
	}

	public Transaction getTransaction() {
		return transaction;
	}

	public Day getStart() {
		return start;
	}

	public void setStart(Day start) {
		this.start = start;
	}

	public boolean isAutomaticExecution() {
		return start != null;
	}

	public Interval getInterval() {
		if (interval == null) {
			return Interval.MONTHLY;
		}
		return interval;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isDeleted() {
		return deleted;
	}

	@Override
	public void setDeleted(boolean deleted) {
		if (this.deleted != deleted) {
			this.deleted = deleted;
			firePropertyChange("deleted", Boolean.valueOf(! deleted), Boolean.valueOf(deleted));
		}
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
