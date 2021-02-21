package de.tomsplayground.peanuts.client.savedtransaction.properties;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PropertyPage;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.widgets.AccountComposite;
import de.tomsplayground.peanuts.client.widgets.DateComposite;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.process.SavedTransaction;
import de.tomsplayground.peanuts.util.Day;

public class SavedTransactionPropertyPage extends PropertyPage {

	private Button automaticExecution;
	private DateComposite startDate;
	private AccountComposite account;

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent,SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		Label label = new Label(composite, SWT.NONE);
		label.setText("Automatic execution");
		automaticExecution = new Button(composite, SWT.CHECK);
		automaticExecution.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				startDate.setEnabled(automaticExecution.getSelection());
				account.setEnabled(automaticExecution.getSelection());
			}
		});

		label = new Label(composite, SWT.NONE);
		label.setText("First execution");
		startDate = new DateComposite(composite, SWT.NONE);

		label = new Label(composite, SWT.NONE);
		label.setText("Account");
		account = new AccountComposite(composite, SWT.NONE, null);

		IAdaptable adapter = getElement();
		SavedTransaction savedTransaction = adapter.getAdapter(SavedTransaction.class);
		automaticExecution.setSelection(savedTransaction.isAutomaticExecution());
		if (savedTransaction.isAutomaticExecution()) {
			startDate.setDay(savedTransaction.getStart());
			account.setAccount(savedTransaction.getAccount());
		} else {
			startDate.setEnabled(false);
			account.setEnabled(false);
		}
		return composite;
	}

	@Override
	public boolean performOk() {
		IAdaptable adapter = getElement();
		SavedTransaction savedTransaction = adapter.getAdapter(SavedTransaction.class);
		SavedTransaction savedTransaction2;
		if (automaticExecution.getSelection()) {
			Day date = startDate.getDay();
			Account a = account.getAccount();
			savedTransaction2 = new SavedTransaction(savedTransaction.getName(), savedTransaction.getTransaction(), date, a);
		} else {
			savedTransaction2 = new SavedTransaction(savedTransaction.getName(), savedTransaction.getTransaction());
		}
		AccountManager accountManager = Activator.getDefault().getAccountManager();
		accountManager.removeSavedTransaction(savedTransaction.getName());
		accountManager.addSavedTransaction(savedTransaction2);
		return super.performOk();
	}

}
