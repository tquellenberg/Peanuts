package de.tomsplayground.peanuts.client.editors.report;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;

public class ReportEditor extends MultiPageEditorPart {

	public static final String ID = "de.tomsplayground.peanuts.client.reportEditor";

	private ChartEditorPart chartEditorPart;
	private MetaEditorPart metaEditorPart;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if ( !(input instanceof ReportEditorInput)) {
			throw new PartInitException("Invalid Input: Must be ReportEditorInput");
		}
		super.init(site, input);
		setPartName(input.getName());
	}

	@Override
	protected void createPages() {
		createEditorPage(new TransactionListEditorPart(), "Report");
		chartEditorPart = new ChartEditorPart();
		createEditorPage(chartEditorPart, "Chart");
		metaEditorPart = new MetaEditorPart();
		createEditorPage(metaEditorPart, "Meta Data");
	}

	private void createEditorPage(IEditorPart editor, String name) {
		try {
			int pageIndex = addPage(editor, getEditorInput());
			setPageText(pageIndex, name);
		} catch (PartInitException e) {
			ErrorDialog.openError(getSite().getShell(), "Error creating nested editor", null,
				e.getStatus());
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		chartEditorPart.doSave(monitor);
		metaEditorPart.doSave(monitor);
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}

	@Override
	public void doSaveAs() {
		// nothing to do
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

}
