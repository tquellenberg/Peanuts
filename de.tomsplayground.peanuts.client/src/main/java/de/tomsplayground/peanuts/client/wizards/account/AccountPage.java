package de.tomsplayground.peanuts.client.wizards.account;

import java.util.Currency;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.Account.Type;

public class AccountPage extends WizardPage {

	private final ModifyListener checkNotEmptyListener = new ModifyListener() {
		@Override
		public void modifyText(ModifyEvent e) {
			Text t = (Text)e.getSource();
			setPageComplete(StringUtils.isNotBlank(t.getText()));
		}
	};
	private Text accountName;
	private Combo type;
	private Combo currency;

	protected AccountPage(String pageName) {
		super(pageName);
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite contents = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		contents.setLayout(layout);
		contents.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label label = new Label(contents, SWT.NONE);
		label.setText("Name:");
		accountName = new Text(contents, SWT.SINGLE | SWT.BORDER);
		accountName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		accountName.addModifyListener(checkNotEmptyListener);

		label = new Label(contents, SWT.NONE);
		label.setText("Currency:");
		currency = new Combo(contents, SWT.READ_ONLY);
		currency.setItems(Activator.getDefault().getCurrencies());

		label = new Label(contents, SWT.NONE);
		label.setText("Type:");
		type = new Combo(contents, SWT.READ_ONLY);
		for (Type t : Account.Type.values()) {
			type.add(t.name());
		}

		setControl(contents);
	}

	public String getAccountName() {
		return accountName.getText().trim();
	}

	public Type getAccountType() {
		return Account.Type.valueOf(type.getText());
	}

	public Currency getCurrency() {
		return Currency.getInstance(currency.getText());
	}

}
