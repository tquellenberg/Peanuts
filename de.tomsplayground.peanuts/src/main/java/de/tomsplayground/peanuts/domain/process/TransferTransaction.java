package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;

import org.apache.log4j.Logger;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.base.Category;
import de.tomsplayground.util.Day;

@XStreamAlias("transfer-transaction")
public class TransferTransaction extends LabeledTransaction {

	private static final Logger log = Logger.getLogger(TransferTransaction.class);

	private ITransferLocation target;
	private TransferTransaction complement;
	private boolean source;

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
		if (memo == null) {
			if (complement.getMemo() != null) {
				complement.setMemo(null);
			}
		} else {
			if ( !memo.equals(complement.getMemo())) {
				complement.setMemo(memo);
			}
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
		if (complement != null && !complement.getAmount().equals(amount)) {
			complement.setAmount(amount);
		}
	}

	public void changeTarget(ITransferLocation newTarget) {
		target.removeTransaction(complement);
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
