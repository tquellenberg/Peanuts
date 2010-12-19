package de.tomsplayground.peanuts.client.editors.report;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.reporting.transaction.Report;

public class ReportEditorFactory implements IElementFactory {

	public static final String ID = "de.tomsplayground.peanuts.client.reportEditorFactoryId";
	public static final String REPORT_NAME = "report.name";

	@Override
	public IAdaptable createElement(IMemento memento) {
		String name = memento.getString(REPORT_NAME);
		AccountManager accountManager = Activator.getDefault().getAccountManager();
		Report report = accountManager.getReport(name);
		if (report == null) {
			report = new Report(name);
			accountManager.addReport(report);
		}
		return new ReportEditorInput(report);
	}

}
