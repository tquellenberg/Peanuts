package de.tomsplayground.peanuts.client.widgets;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.Account;

public class AccountComposite extends Composite {

	private Button accountButton;
	private Combo accountCombo;

	public AccountComposite(Composite parent, int style, Account disabledAccount) {
		super(parent, style);
		GridLayout gridLayout = new GridLayout(2, false);
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 0;
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		setLayout(gridLayout);

		accountButton = new Button(this, SWT.FLAT);
		Image image = Activator.getDefault().getImageRegistry().get(Activator.IMAGE_ACCOUNT);
		accountButton.setImage(image);

		accountCombo = new Combo(this, SWT.READ_ONLY);
		accountCombo.add("");
		List<Account> accounts = Activator.getDefault().getAccountManager().getAccounts();
		for (Account acc : accounts) {
			if (acc != disabledAccount)
				accountCombo.add(acc.getName());
		}
		accountCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	}

	public void setAccount(Account account) {
		if (account == null) {
			accountCombo.setText("");
		} else {
			accountCombo.setText(account.getName());
		}
	}

	public Account getAccount() {
		ImmutableList<Account> accounts = Activator.getDefault().getAccountManager().getAccounts();
		String accountName = accountCombo.getText();
		for (Account acc : accounts) {
			if (acc.getName().equals(accountName))
				return acc;
		}
		return null;
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		accountButton.setEnabled(enabled);
		accountCombo.setEnabled(enabled);
	}

	public void addSelectionListener(SelectionListener listener) {
		accountCombo.addSelectionListener(listener);
	}

	public void removeSelectionListener(SelectionListener listener) {
		accountCombo.removeSelectionListener(listener);
	}

}
