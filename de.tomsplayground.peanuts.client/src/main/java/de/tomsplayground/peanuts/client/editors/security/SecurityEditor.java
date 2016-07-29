package de.tomsplayground.peanuts.client.editors.security;

import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;

import de.tomsplayground.peanuts.client.app.Activator;

public class SecurityEditor extends MultiPageEditorPart {

	public static final String ID = "de.tomsplayground.peanuts.client.securityEditor";

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if ( !(input instanceof SecurityEditorInput)) {
			throw new PartInitException("Invalid Input: Must be SecurityEditorInput");
		}
		super.init(site, input);
		setPartName(input.getName());
	}

	@Override
	protected void createPages() {
		createEditorPage(new ChartEditorPart(), "Chart");
		createEditorPage(new PriceEditorPart(), "Prices");
		createEditorPage(new FundamentalDataEditorPart(), "Fundamentals");
		createEditorPage(new ScrapingEditorPart(), "Scraping");
		createEditorPage(new DevelopmentEditorPart(), "Development");
		createEditorPage(new NotesEditorPart(), "Notes");
	}

	private void createEditorPage(IEditorPart editor, String name) {
		try {
			int pageIndex = addPage(editor, getEditorInput());
			setPageText(pageIndex, name);
		} catch (PartInitException e) {
			ErrorDialog.openError(getSite().getShell(), "Error creating nested editor", null, e.getStatus());
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		int pageCount = getPageCount();
		for (int i = 0; i < pageCount; i++) {
			getEditor(i).doSave(monitor);
		}
		try {
			Activator.getDefault().save(Activator.getDefault().getFilename());
		} catch (IOException e) {
			e.printStackTrace();
		}
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
