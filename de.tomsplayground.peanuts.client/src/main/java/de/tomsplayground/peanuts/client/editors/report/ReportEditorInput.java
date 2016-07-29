package de.tomsplayground.peanuts.client.editors.report;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

import de.tomsplayground.peanuts.client.editors.ITransactionProviderInput;
import de.tomsplayground.peanuts.config.IConfigurable;
import de.tomsplayground.peanuts.domain.base.ITransactionProvider;
import de.tomsplayground.peanuts.domain.reporting.transaction.Report;

public class ReportEditorInput implements IEditorInput, ITransactionProviderInput {

	private final Report report;

	public ReportEditorInput(Report report) {
		this.report = report;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		return report.equals(((ReportEditorInput) obj).report);
	}

	@Override
	public int hashCode() {
		return report.hashCode();
	}

	@Override
	public String getName() {
		return report.getName();
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageDescriptor.getMissingImageDescriptor();
	}

	@Override
	public IPersistableElement getPersistable() {
		return new IPersistableElement() {

			@Override
			public String getFactoryId() {
				return ReportEditorFactory.ID;
			}

			@Override
			public void saveState(IMemento memento) {
				memento.putString(ReportEditorFactory.REPORT_NAME, report.getName());
			}
		};
	}

	@Override
	public String getToolTipText() {
		return report.getName();
	}

	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		if (adapter == IConfigurable.class) {
			return report;
		}
		return null;
	}

	public Report getReport() {
		return report;
	}

	@Override
	public ITransactionProvider getTransactionProvider() {
		return report;
	}

}
