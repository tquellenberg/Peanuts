package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.base.Category;
import de.tomsplayground.peanuts.util.Day;

@XStreamAlias("transfer-transaction")
public class TransferTransaction extends LabeledTransaction {

	private final static Logger log = LoggerFactory.getLogger(TransferTransaction.class);

	private ITransferLocation target;
	private TransferTransaction complement;
	private final boolean source;

	protected TransferTransaction(Day date, BigDecimal value, ITransferLocation target,
		boolean source) {
		super(date, value, "");
		this.target = target;
		this.source = source;
	}

	@Override
	public void setCategory(Category category) {
		super.setCategory(category);
		if (category == null) {
			if (complement.getCategory() != null) {
				complement.setCategory(null);
			}
		} else {
			if ( !category.equals(complement.getCategory())) {
				complement.setCategory(category);
			}
		}
	}

	@Override
	public void setDay(Day day) {
		super.setDay(day);
		if (complement != null && (complement.getDay() == null || !complement.getDay().equals(day))) {
			complement.setDay(day);
		}
	}

	@Override
	public void setMemo(String memo) {
		super.setMemo(memo);
		if (! getMemo().equals(complement.getMemo())) {
			complement.setMemo(getMemo());
		}
	}

	@Override
	public void setAmount(BigDecimal amount) {
		super.setAmount(amount);
		if (complement != null && !target.getCurrency().equals(complement.getTarget().getCurrency())) {
			log.warn("setAmount for TransferTransaction between different currencies");
			return;
		}
		amount = amount.negate();
		if (complement != null && complement.getAmount().compareTo(amount) != 0) {
			complement.setAmount(amount);
		}
	}

	public void changeTarget(ITransferLocation newTarget) {
		complement.setComplement(null);
		target.removeTransaction(complement);
		complement.setComplement(this);
		newTarget.addTransaction(complement);
		target = newTarget;
	}

	public ITransferLocation getTarget() {
		return target;
	}

	public TransferTransaction getComplement() {
		return complement;
	}

	public void setComplement(TransferTransaction complement) {
		this.complement = complement;
	}

	public boolean isSource() {
		return source;
	}

	@Override
	public Object clone() {
		TransferTransaction clone = (TransferTransaction) super.clone();
		if (complement != null) {
			complement.setComplement(null);
			TransferTransaction ct = (TransferTransaction) complement.clone();
			complement.setComplement(this);
			target.addTransaction(ct);
			ct.setComplement(clone);
			clone.setComplement(ct);
		}
		return clone;
	}

}
