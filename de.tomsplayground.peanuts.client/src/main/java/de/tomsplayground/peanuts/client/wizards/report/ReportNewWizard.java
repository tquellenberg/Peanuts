package de.tomsplayground.peanuts.client.wizards.report;

import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.editors.report.ReportEditor;
import de.tomsplayground.peanuts.client.editors.report.ReportEditorInput;
import de.tomsplayground.peanuts.domain.base.Category;
import de.tomsplayground.peanuts.domain.query.CategoryQuery;
import de.tomsplayground.peanuts.domain.query.DateQuery;
import de.tomsplayground.peanuts.domain.reporting.transaction.Report;

public class ReportNewWizard extends Wizard implements INewWizard {

	public static final String ID = "de.tomsplayground.peanuts.client.reportNewWizard";

	private AccountsPage accountsPage;
	private CategoriesPage categoriesPage;
	private DatesPage datesPage;

	private IWorkbench workbench;

	@Override
	public void init(IWorkbench w, IStructuredSelection selection) {
		this.workbench = w;
	}

	@Override
	public void addPages() {
		accountsPage = new AccountsPage("Accounts");
		addPage(accountsPage);
		categoriesPage = new CategoriesPage("Categories");
		addPage(categoriesPage);
		datesPage = new DatesPage("Dates");
		addPage(datesPage);
	}

	@Override
	public boolean performFinish() {
		Report report = new Report(datesPage.getReportName());
		report.setAccounts(accountsPage.getAccounts());

		List<Category> categories = categoriesPage.getCategories();
		if (categories != null)
			report.addQuery(new CategoryQuery(categories));

		DateQuery query = datesPage.getDateQuery();
		report.addQuery(query);

		Activator.getDefault().getAccountManager().addReport(report);

		ReportEditorInput input = new ReportEditorInput(report);
		IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
		try {
			activeWorkbenchWindow.getActivePage().openEditor(input, ReportEditor.ID);
			return true;
		} catch (PartInitException e) {
			MessageDialog.openError(activeWorkbenchWindow.getShell(), "Error",
				"Error opening editor:" + e.getMessage());
		}
		return false;
	}

}
