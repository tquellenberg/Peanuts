package de.tomsplayground.peanuts.client.savedtransaction.properties;

import java.util.Currency;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import de.tomsplayground.peanuts.client.widgets.DateComposite;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.process.SavedTransaction;
import de.tomsplayground.peanuts.domain.process.SavedTransaction.Interval;
import de.tomsplayground.peanuts.domain.process.Transaction;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class SavedTransactionPropertyPage extends PropertyPage {

	private final static Logger log = LoggerFactory.getLogger(SavedTransactionPropertyPage.class);

	private Button automaticExecution;
	private DateComposite startDate;
	private Combo intervalCombo;

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
		label = new Label(composite, SWT.NONE);
		if (account !=  null) {
			label.setText(account.getName());
		}

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
		SavedTransaction savedTransaction2;
		if (automaticExecution.getSelection()) {
			Day date = startDate.getDay();
			Interval interval = SavedTransaction.Interval.values()[intervalCombo.getSelectionIndex()];
			savedTransaction2 = new SavedTransaction(savedTransaction.getName(), savedTransaction.getTransaction(), date, interval);
		} else {
			savedTransaction2 = new SavedTransaction(savedTransaction.getName(), savedTransaction.getTransaction());
		}
		AccountManager accountManager = Activator.getDefault().getAccountManager();
		accountManager.removeSavedTransaction(savedTransaction);
		accountManager.addSavedTransaction(savedTransaction2);

		if (savedTransaction2.isAutomaticExecution()) {
			SavedTransactionManager.createFuturTransaction(savedTransaction2, 90);
		}

		return super.performOk();
	}

}
