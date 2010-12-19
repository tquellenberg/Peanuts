package de.tomsplayground.peanuts.client.wizards.credit;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.process.ICredit;

public class CreditNewWizard extends Wizard implements INewWizard {

	public static final String ID = "de.tomsplayground.peanuts.client.creditNewWizard";
	
	private BasePage basePage;
	
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// nothing to do
	}
	
	@Override
	public void addPages() {
		basePage = new BasePage("Credit");
		addPage(basePage);
	}

	@Override
	public boolean performFinish() {
		ICredit credit = basePage.getCredit();
		if (credit != null) {
			Activator.getDefault().getAccountManager().addCredit(credit);
		}
		return credit != null;
	}


}
