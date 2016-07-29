package de.tomsplayground.peanuts.client.actions;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

public class PasswordDialog extends InputDialog {

	private String password;

	public PasswordDialog(Shell parentShell, String dialogTitle, String dialogMessage) {
		super(parentShell, dialogTitle, dialogMessage, "", null);
	}

	@Override
	protected int getInputTextStyle() {
		return SWT.PASSWORD | SWT.BORDER;
	}

	@Override
	protected void okPressed() {
		password = getText().getText();
		super.okPressed();
	}

	public String getPassword() {
		return password;
	}
}
