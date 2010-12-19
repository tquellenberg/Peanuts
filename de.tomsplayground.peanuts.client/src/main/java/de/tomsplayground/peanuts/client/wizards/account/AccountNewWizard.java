package de.tomsplayground.peanuts.client.wizards.account;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.editors.account.AccountEditor;
import de.tomsplayground.peanuts.client.editors.account.AccountEditorInput;
import de.tomsplayground.peanuts.domain.base.Account;


public class AccountNewWizard extends Wizard implements INewWizard {

	public static final String ID = "de.tomsplayground.peanuts.client.accountNewWizard";
	
	private AccountPage accountPage;
	private IWorkbench workbench;

	@Override
	public void init(IWorkbench w, IStructuredSelection selection) {
		this.workbench = w;
	}

	@Override
	public void addPages() {
		accountPage = new AccountPage("");
		addPage(accountPage);
	}
	
	@Override
	public boolean performFinish() {
		String accontName = accountPage.getAccountName();
		Account.Type type = accountPage.getAccountType();
		Account account = Activator.getDefault().getAccountManager().getOrCreateAccount(accontName, type);
		account.setCurrency(accountPage.getCurrency());
		IEditorInput input = new AccountEditorInput(account);
		IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
		try {
			activeWorkbenchWindow.getActivePage().openEditor(input, AccountEditor.ID);
			return true;
		} catch (PartInitException e) {
			MessageDialog.openError(activeWorkbenchWindow.getShell(), "Error",
					"Error opening editor:" + e.getMessage());
		}
		return false;
	}

}
