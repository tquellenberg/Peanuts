package de.tomsplayground.peanuts.client.wizards.security;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.editors.security.SecurityEditor;
import de.tomsplayground.peanuts.client.editors.security.SecurityEditorInput;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Security;

public class SecurityNewWizard extends Wizard implements INewWizard {

	public static final String ID = "de.tomsplayground.peanuts.client.securityNewWizard";

	private SecurityNewWizardPage page;

	private IWorkbenchWindow workbenchWindow;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		workbenchWindow = workbench.getActiveWorkbenchWindow();
	}

	@Override
	public void addPages() {
		page = new SecurityNewWizardPage("");
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		AccountManager accountManager = Activator.getDefault().getAccountManager();
		Security security = accountManager.getOrCreateSecurity(page.getSecurityName());
		security.setTicker(page.getSecurityTicker());
		security.setISIN(page.getIsin());
		SecurityEditorInput input = new SecurityEditorInput(security);
		try {
			workbenchWindow.getActivePage().openEditor(input, SecurityEditor.ID);
			return true;
		} catch (PartInitException e) {
			MessageDialog.openError(workbenchWindow.getShell(), "Error", "Error opening editor:" +
				e.getMessage());
		}
		return false;
	}

}
