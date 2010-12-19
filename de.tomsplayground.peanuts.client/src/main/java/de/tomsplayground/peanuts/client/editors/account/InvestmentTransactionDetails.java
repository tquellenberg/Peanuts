package de.tomsplayground.peanuts.client.editors.account;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Calendar;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.fieldassist.AutoCompleteField;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.widgets.CalculatorText;
import de.tomsplayground.peanuts.client.widgets.CategoryComposite;
import de.tomsplayground.peanuts.client.widgets.DateComposite;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.Category;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction.Type;
import de.tomsplayground.peanuts.domain.process.Transaction;
import de.tomsplayground.peanuts.util.PeanutsUtil;
import de.tomsplayground.util.Day;

public class InvestmentTransactionDetails implements ITransactionDetail {

	private Account account;
	InvestmentTransaction transaction;
	Transaction parentTransaction;

	private Text security;
	private DateComposite date;
	private Text memo;
	private CategoryComposite categoryComposite;
	private Text quantity;
	private Text price;
	private Text commission;
	private Text amount;
	private Button cancel;
	private Button okay;
	private Composite detailComposit;
	private Combo transactionType;
	private boolean internalUpdate;

	private ModifyListener modifyListener = new ModifyListener() {
		@Override
		public void modifyText(ModifyEvent arg0) {
			if ( !internalUpdate) {
				cancel.setEnabled(true);
				okay.setEnabled(true);
			}
		}
	};

	private ModifyListener recalculateAmount = new ModifyListener() {
		@Override
		public void modifyText(ModifyEvent arg0) {
			if ( !internalUpdate) {
				try {
					Type type = getTransactionType();
					BigDecimal newQuantity = PeanutsUtil.parseCurrency(quantity.getText());
					BigDecimal newPrice = PeanutsUtil.parseCurrency(price.getText());
					BigDecimal newCommission = PeanutsUtil.parseCurrency(commission.getText());
					BigDecimal newAmount = InvestmentTransaction.calculateAmount(type, newPrice,
						newQuantity, newCommission);
					amount.setText(PeanutsUtil.formatCurrency(newAmount, null));
				} catch (ParseException e) {
					amount.setText("");
				}
			}
		}
	};
	
	public InvestmentTransactionDetails(Account account) {
		this.account = account;
	}

	@Override
	public Composite createComposite(Composite parent) {
		detailComposit = new Composite(parent, SWT.NONE);
		detailComposit.setLayout(new GridLayout());

		Group group = new Group(detailComposit, SWT.NONE);
		group.setText("Transaction details");
		group.setLayout(new GridLayout(2, true));
		group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		List<Security> securities = Activator.getDefault().getAccountManager().getSecurities();
		String securityNames[] = new String[securities.size()];
		int i = 0;
		for (Security sec : securities) {
			securityNames[i++] = sec.getName();
		}
		security = new Text(group, SWT.SINGLE | SWT.BORDER);
		@SuppressWarnings("unused")
		AutoCompleteField autoCompleteSecurity = new AutoCompleteField(security, new TextContentAdapter(), securityNames);
		security.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		date = new DateComposite(group, SWT.NONE);

		memo = new Text(group, SWT.MULTI | SWT.WRAP | SWT.BORDER);
		GridData ldata = new GridData(SWT.FILL, SWT.CENTER, true, false);
		ldata.heightHint = memo.getLineHeight() * 3;
		memo.setLayoutData(ldata);

		categoryComposite = new CategoryComposite(group, SWT.NONE);
		categoryComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Composite box = new Composite(group, SWT.NONE);
		box.setLayout(new FillLayout(SWT.HORIZONTAL));

		transactionType = new Combo(box, SWT.READ_ONLY);
		transactionType.add("Sell");
		transactionType.add("Buy");
		transactionType.add("Expense");
		transactionType.add("Income");

		quantity = new Text(box, SWT.SINGLE | SWT.BORDER);
		price = new Text(box, SWT.SINGLE | SWT.BORDER);
		commission = (new CalculatorText(box, SWT.SINGLE | SWT.BORDER)).getText();

		amount = new Text(group, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		amount.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		Composite buttons = new Composite(detailComposit, SWT.NONE);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.END;
		buttons.setLayoutData(gridData);
		buttons.setLayout(new FillLayout(SWT.HORIZONTAL));
		cancel = new Button(buttons, SWT.NONE);
		cancel.setText("Cancel");
		cancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setInput(transaction, parentTransaction);
			}
		});
		okay = new Button(buttons, SWT.NONE);
		okay.setText("Save");
		okay.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				readForm();
				cancel.setEnabled(false);
				okay.setEnabled(false);
			}
		});

		quantity.addModifyListener(recalculateAmount);
		quantity.addModifyListener(modifyListener);
		price.addModifyListener(recalculateAmount);
		price.addModifyListener(modifyListener);
		commission.addModifyListener(recalculateAmount);
		commission.addModifyListener(modifyListener);
		categoryComposite.addModifyListener(modifyListener);
		memo.addModifyListener(modifyListener);
		date.addModifyListener(modifyListener);
		transactionType.addModifyListener(recalculateAmount);
		transactionType.addModifyListener(modifyListener);
		security.addModifyListener(modifyListener);

		return detailComposit;
	}

	protected void readForm() {
		try {
			String securityName = security.getText();
			Security newSecurity = Activator.getDefault().getAccountManager().getOrCreateSecurity(
				securityName);
			Category newCategory = categoryComposite.getCategory();
			Day newDate = date.getDay();
			BigDecimal newQuantity = PeanutsUtil.parseCurrency(quantity.getText());
			BigDecimal newPrice = PeanutsUtil.parseCurrency(price.getText());
			BigDecimal newCommission = PeanutsUtil.parseCurrency(commission.getText());
			Type type = getTransactionType();
			if (transaction == null) {
				transaction = new InvestmentTransaction(newDate, newSecurity, newPrice,
					newQuantity, newCommission, type);
				if (parentTransaction != null) {
					parentTransaction.addSplit(transaction);
				} else {
					account.addTransaction(transaction);
				}
			} else {
				transaction.setSecurity(newSecurity);
				transaction.setInvestmentDetails(type, newPrice, newQuantity, newCommission);
				transaction.setMemo(memo.getText());
				transaction.setCategory(newCategory);
				transaction.setDay(newDate);
			}
		} catch (ParseException e) {
			IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage());
			(new ErrorDialog(detailComposit.getShell(), "Format Error", "Input not valid.", status,
				IStatus.ERROR)).open();
		}
	}

	private Type getTransactionType() {
		Type type;
		if (transactionType.getSelectionIndex() == 0) {
			type = InvestmentTransaction.Type.SELL;
		} else if (transactionType.getSelectionIndex() == 1) {
			type = InvestmentTransaction.Type.BUY;
		} else if (transactionType.getSelectionIndex() == 2) {
			type = InvestmentTransaction.Type.EXPENSE;
		} else {
			type = InvestmentTransaction.Type.INCOME;
		}
		return type;
	}

	@Override
	public void setInput(Transaction transaction, Transaction parentTransaction) {
		internalUpdate = true;
		this.transaction = (InvestmentTransaction) transaction;
		this.parentTransaction = parentTransaction;
		if (transaction == null) {
			date.setDate(Calendar.getInstance());
			memo.setText("");
			categoryComposite.setCategory(null);
			security.setText("");
			quantity.setText("");
			price.setText("");
			commission.setText("");
			amount.setText("");
			transactionType.select(0);
		} else {
			date.setDay(transaction.getDay());
			memo.setText(transaction.getMemo() != null ? transaction.getMemo() : "");
			categoryComposite.setCategory(transaction.getCategory());
			if (this.transaction.getSecurity() != null) {
				security.setText(this.transaction.getSecurity().getName());
			} else {
				security.setText("");
			}
			if (this.transaction.getQuantity() != null) {
				quantity.setText(PeanutsUtil.formatQuantity(this.transaction.getQuantity()));
			} else {
				quantity.setText("");
			}
			if (this.transaction.getPrice() != null) {
				price.setText(PeanutsUtil.formatCurrency(this.transaction.getPrice(), null));
			} else {
				price.setText("");
			}
			if (this.transaction.getCommission() != null) {
				commission.setText(PeanutsUtil.formatCurrency(this.transaction.getCommission(),
					null));
			} else {
				commission.setText("");
			}
			amount.setText(PeanutsUtil.formatCurrency(transaction.getAmount(), null));
			switch (this.transaction.getType()) {
				case SELL:
					transactionType.select(0);
					break;
				case BUY:
					transactionType.select(1);
					break;
				case EXPENSE:
					transactionType.select(2);
					break;
				case INCOME:
					transactionType.select(3);
					break;
			}

			cancel.setEnabled(false);
			okay.setEnabled(false);
		}
		internalUpdate = false;
	}
	
	@Override
	public Composite getComposite() {
		return detailComposit;
	}

}
