package de.tomsplayground.peanuts.client.wizards.securitycategory;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.statistics.SecurityCategoryMapping;

public class SecurityCategoryNewWizard extends Wizard implements INewWizard {

	public static final String ID = "de.tomsplayground.peanuts.client.securityCategoryNewWizard";

	private SecurityCategoryNewWizardPage page;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// nothing to do
	}

	@Override
	public void addPages() {
		page = new SecurityCategoryNewWizardPage("");
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		AccountManager accountManager = Activator.getDefault().getAccountManager();
		SecurityCategoryMapping securityCategoryMapping = new SecurityCategoryMapping(page.getName(), page.getCategories());
		accountManager.addSecurityCategoryMapping(securityCategoryMapping);
		return true;
	}

}
