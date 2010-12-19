package de.tomsplayground.peanuts.client.editors.credit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;


public class CreditEditor extends MultiPageEditorPart {
	
	public static final String ID = "de.tomsplayground.peanuts.client.creditEditor";

	private CreditChartEditorPart chartEditorPart;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if ( !(input instanceof CreditEditorInput)) {
			throw new PartInitException("Invalid Input: Must be CreditEditorInput");
		}
		super.init(site, input);
		setPartName(input.getName());
	}

	@Override
	protected void createPages() {
		chartEditorPart = new CreditChartEditorPart();
		createEditorPage(chartEditorPart, "Credit");
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
