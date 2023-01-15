package de.tomsplayground.peanuts.client.editors.account;

import java.math.BigDecimal;
import java.text.ParseException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.widgets.AccountComposite;
import de.tomsplayground.peanuts.client.widgets.CalculatorText;
import de.tomsplayground.peanuts.client.widgets.CategoryComposite;
import de.tomsplayground.peanuts.client.widgets.DateComposite;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.Category;
import de.tomsplayground.peanuts.domain.process.BankTransaction;
import de.tomsplayground.peanuts.domain.process.LabeledTransaction;
import de.tomsplayground.peanuts.domain.process.Transaction;
import de.tomsplayground.peanuts.domain.process.Transfer;
import de.tomsplayground.peanuts.domain.process.TransferTransaction;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class TransactionDetails implements ITransactionDetail {

	private final Account account;
	private Transaction transaction;
	private Transaction parentTransaction;

	private Text label;
	private DateComposite dateComposite;
	private Text memo;
	private Text amount;
	private Button cancel;
	private Button okay;
	private Composite detailComposit;
	private CategoryComposite categoryComposite;
	private AccountComposite accountComposite;

	private final ModifyListener modifyListener = new ModifyListener() {
		@Override
		public void modifyText(ModifyEvent arg0) {
			cancel.setEnabled(true);
			okay.setEnabled(true);
		}
	};

	public TransactionDetails(Account account) {
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

		label = new Text(group, SWT.SINGLE | SWT.BORDER);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		dateComposite = new DateComposite(group, SWT.NONE);

		memo = new Text(group, SWT.MULTI | SWT.WRAP | SWT.BORDER);
		GridData ldata = new GridData(SWT.FILL, SWT.CENTER, true, false);
		ldata.heightHint = memo.getLineHeight() * 3;
		memo.setLayoutData(ldata);

		Composite composite = new Composite(group, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 0;
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		composite.setLayout(gridLayout);

		categoryComposite = new CategoryComposite(composite, SWT.NONE);
		categoryComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		accountComposite = new AccountComposite(composite, SWT.NONE, account);
		accountComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		new Label(group, SWT.NONE);

		amount = (new CalculatorText(group, SWT.SINGLE | SWT.BORDER)).getText();
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

		amount.addModifyListener(modifyListener);
		categoryComposite.addModifyListener(modifyListener);
		accountComposite.addSelectionListener(new SelectionListener(){

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				cancel.setEnabled(true);
				okay.setEnabled(true);
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				cancel.setEnabled(true);
				okay.setEnabled(true);
			}});
		memo.addModifyListener(modifyListener);
		dateComposite.addModifyListener(modifyListener);
		label.addModifyListener(modifyListener);

		return detailComposit;
	}

	@Override
	public void dispose() {
		// nothing to do
	}

	protected void readForm() {
		try {
			BigDecimal newAmount = PeanutsUtil.parseCurrency(amount.getText());
			String newMemo = memo.getText();
			String newLabel = label.getText();
			Category newCategory = categoryComposite.getCategory();
			Day newDate = dateComposite.getDay();

			if (transaction == null) {
				// Create new transaction
				if (accountComposite.getAccount() != null && accountComposite.getAccount() != account) {
					Transfer transfer = new Transfer(account, accountComposite.getAccount(), newAmount, newDate);
					transfer.setMemo(newMemo);
					transfer.setLabel(newLabel);
					transfer.setCategory(newCategory);
					accountComposite.getAccount().addTransaction(transfer.getTransferTo());
					transaction = transfer.getTransferFrom();
				} else {
					transaction = new BankTransaction(newDate, newAmount, newLabel);
					transaction.setMemo(newMemo);
					transaction.setCategory(newCategory);
					((LabeledTransaction) transaction).setLabel(newLabel);
				}
				if (parentTransaction != null) {
					parentTransaction.addSplit(transaction);
				} else {
					account.addTransaction(transaction);
				}
			} else {
				// Change existing transaction
				if (transaction.getSplits().isEmpty()) {
					transaction.setAmount(newAmount);
				}
				transaction.setMemo(newMemo);
				transaction.setCategory(newCategory);
				transaction.setDay(newDate);
				if (transaction instanceof LabeledTransaction labledT) {
					labledT.setLabel(newLabel);
				}
				if (transaction instanceof TransferTransaction tt) {
					if (accountComposite.getAccount() == null || accountComposite.getAccount() == account) {
						// From transfer to normal transaction
						TransferTransaction complement = tt.getComplement();
						if (complement.getComplement() == tt) {
							try {
								tt.getTarget().removeTransaction(complement);
							} catch (IllegalArgumentException e) {
								// Ignore inconsistent state
							}
						}
						if (parentTransaction != null) {
							parentTransaction.removeSplit(tt);
						} else {
							account.removeTransaction(tt);
						}
						BankTransaction tnew = new BankTransaction(tt.getDay(), tt.getAmount(), tt.getLabel());
						tnew.setCategory(tt.getCategory());
						tnew.setMemo(tt.getMemo());
						transaction = tnew;
						if (parentTransaction != null) {
							parentTransaction.addSplit(transaction);
						} else {
							account.addTransaction(transaction);
						}
					} else if (tt.getTarget() != accountComposite.getAccount()) {
						// New Transfer-Target
						tt.changeTarget(accountComposite.getAccount());
					}
				} else {
					if (accountComposite.getAccount() != null && accountComposite.getAccount() != account) {
						// From normal transaction to transfer
						Transfer transfer = new Transfer(account, accountComposite.getAccount(), transaction.getAmount().negate(), transaction.getDay());
						transfer.setCategory(transaction.getCategory());
						transfer.setMemo(transaction.getMemo());
						transfer.setLabel(newLabel);
						if (parentTransaction != null) {
							parentTransaction.removeSplit(transaction);
						} else {
							account.removeTransaction(transaction);
						}
						accountComposite.getAccount().addTransaction(transfer.getTransferTo());
						transaction = transfer.getTransferFrom();
						if (parentTransaction != null) {
							parentTransaction.addSplit(transaction);
						} else {
							account.addTransaction(transaction);
						}
					}
				}
			}
		} catch (ParseException e) {
			IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage());
			(new ErrorDialog(detailComposit.getShell(), "Format Error", "Input not valid.", status,
				IStatus.ERROR)).open();
		}
	}

	@Override
	public void setInput(Transaction transaction, Transaction parentTransaction) {
		this.transaction = transaction;
		this.parentTransaction = parentTransaction;
		if (transaction == null) {
			dateComposite.setDay(Day.today());
			categoryComposite.setCategory(null);
			memo.setText("");
			label.setText("");
			amount.setText("");
			accountComposite.setAccount(null);
			amount.setEditable(true);
		} else {
			dateComposite.setDay(transaction.getDay());
			memo.setText(transaction.getMemo() != null ? transaction.getMemo() : "");
			categoryComposite.setCategory(transaction.getCategory());

			if (transaction instanceof LabeledTransaction bankTransaction) {
				label.setText(bankTransaction.getLabel() != null ? bankTransaction.getLabel() : "");
				label.setEnabled(true);
			} else {
				label.setText("");
				label.setEnabled(false);
			}
			if (transaction instanceof TransferTransaction transferT) {
				accountComposite.setAccount((Account)transferT.getTarget());
			} else {
				accountComposite.setAccount(null);
			}
			amount.setText(PeanutsUtil.formatCurrency(transaction.getAmount(), null));
			cancel.setEnabled(false);
			okay.setEnabled(false);
			amount.setEditable(transaction.getSplits().isEmpty());
		}
	}

	@Override
	public Composite getComposite() {
		return detailComposit;
	}

	@Override
	public void setFocus() {
		amount.setFocus();
	}

}
