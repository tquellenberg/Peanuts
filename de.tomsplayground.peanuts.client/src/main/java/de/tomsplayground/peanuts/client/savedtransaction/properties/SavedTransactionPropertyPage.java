package de.tomsplayground.peanuts.client.savedtransaction.properties;

import java.util.Currency;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PropertyPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.savedtransaction.SavedTransactionManager;
import de.tomsplayground.peanuts.client.widgets.AccountComposite;
import de.tomsplayground.peanuts.client.widgets.DateComposite;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.process.SavedTransaction;
import de.tomsplayground.peanuts.domain.process.SavedTransaction.Interval;
import de.tomsplayground.peanuts.domain.process.Transaction;
import de.tomsplayground.peanuts.domain.process.TransferTransaction;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class SavedTransactionPropertyPage extends PropertyPage {

	private final static Logger log = LoggerFactory.getLogger(SavedTransactionPropertyPage.class);

	private Button automaticExecution;
	private DateComposite startDate;
	private Combo intervalCombo;

	private AccountComposite accountComposite;

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent,SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		IAdaptable adapter = getElement();
		SavedTransaction savedTransaction = adapter.getAdapter(SavedTransaction.class);
		Transaction transaction = savedTransaction.getTransaction();
		Account account = SavedTransactionManager.getAccountForTransaction(transaction);
		log.info("savedTransaction: {}", savedTransaction);

		Label label;

		label = new Label(composite, SWT.NONE);
		label.setText("Amount");
		label = new Label(composite, SWT.NONE);
		Currency currency = null;
		if (account != null) {
			currency = account.getCurrency();
		}
		label.setText(PeanutsUtil.formatCurrency(transaction.getAmount(), currency));

		label = new Label(composite, SWT.NONE);
		label.setText("Account");
		accountComposite = new AccountComposite(composite, SWT.NONE, null);
		accountComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		accountComposite.setAccount(account);

		label = new Label(composite, SWT.NONE);
		label.setText("Automatic execution");
		automaticExecution = new Button(composite, SWT.CHECK);
		automaticExecution.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				startDate.setEnabled(automaticExecution.getSelection());
				intervalCombo.setEnabled(automaticExecution.getSelection());
			}
		});

		label = new Label(composite, SWT.NONE);
		label.setText("Next execution");
		startDate = new DateComposite(composite, SWT.NONE);

		label = new Label(composite, SWT.NONE);
		label.setText("Interval");
		intervalCombo = new Combo(composite, SWT.READ_ONLY);
		intervalCombo.add("monthly");
		intervalCombo.add("quarterly");
		intervalCombo.add("half yearly");
		intervalCombo.add("yearly");

		automaticExecution.setSelection(savedTransaction.isAutomaticExecution());
		if (savedTransaction.isAutomaticExecution()) {
			startDate.setDay(savedTransaction.getStart());
			intervalCombo.select(savedTransaction.getInterval().ordinal());
		} else {
			startDate.setEnabled(false);
			intervalCombo.setEnabled(false);
		}
		return composite;
	}

	@Override
	public boolean performOk() {
		IAdaptable adapter = getElement();
		SavedTransaction savedTransaction = adapter.getAdapter(SavedTransaction.class);
		SavedTransaction updatedSavedTransaction;
				
		if (automaticExecution.getSelection()) {
			Day date = startDate.getDay();
			Interval interval = SavedTransaction.Interval.values()[intervalCombo.getSelectionIndex()];
			updatedSavedTransaction = new SavedTransaction(savedTransaction.getName(), savedTransaction.getTransaction(), date, interval);
		} else {
			updatedSavedTransaction = new SavedTransaction(savedTransaction.getName(), savedTransaction.getTransaction());
		}
		
		Account currentAccount = SavedTransactionManager.getAccountForTransaction(savedTransaction.getTransaction());
		Account newAccount = accountComposite.getAccount();
		if (currentAccount != newAccount) {
			Transaction newTransaction = (Transaction) savedTransaction.getTransaction().clone();
			newTransaction.setDay(startDate.getDay());
			if (newTransaction instanceof TransferTransaction tt) {
				currentAccount.addTransaction(newTransaction);
				tt.getComplement().changeTarget(newAccount);
			} else {
				newAccount.addTransaction(newTransaction);
			}
			updatedSavedTransaction.setTransaction(newTransaction);
		}
		
		AccountManager accountManager = Activator.getDefault().getAccountManager();
		accountManager.removeSavedTransaction(accountManager.getSavedTransaction(savedTransaction.getName()));
		accountManager.addSavedTransaction(updatedSavedTransaction);

		if (updatedSavedTransaction.isAutomaticExecution()) {
			SavedTransactionManager.createFuturTransaction(updatedSavedTransaction, 90);
		}

		return super.performOk();
	}

}
