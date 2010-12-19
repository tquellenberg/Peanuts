package de.tomsplayground.peanuts.client.editors.security;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableEditor;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;

public class SecurityEditor extends MultiPageEditorPart implements IPersistableEditor {

	public static final String ID = "de.tomsplayground.peanuts.client.securityEditor";

	private IMemento mementoForRestore;
	private List<IEditorPart> editors = new ArrayList<IEditorPart>();

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
		createEditorPage(new DevelopmentEditorPart(), "Development");
	}

	private void createEditorPage(IEditorPart editor, String name) {
		try {
			if (editor instanceof IPersistableEditor) {
				IPersistableEditor pEditor = (IPersistableEditor) editor;
				if (mementoForRestore != null) {
					IMemento childMemento = mementoForRestore.getChild(editor.getClass().getName());
					if (childMemento != null) {
						pEditor.restoreState(childMemento);
					}
				}
			}
			int pageIndex = addPage(editor, getEditorInput());
			setPageText(pageIndex, name);
			editors.add(editor);
		} catch (PartInitException e) {
			ErrorDialog.openError(getSite().getShell(), "Error creating nested editor", null,
				e.getStatus());
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		int pageCount = getPageCount();
		for (int i = 0; i < pageCount; i++)
			getEditor(i).doSave(monitor);
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

	@Override
	public void restoreState(IMemento memento) {
		// Editors not yet created.
		this.mementoForRestore = memento;
	}

	@Override
	public void saveState(IMemento memento) {
		for (IEditorPart editor : editors) {
			if (editor instanceof IPersistableEditor) {
				IPersistableEditor pEditor = (IPersistableEditor) editor;
				pEditor.saveState(memento.createChild(editor.getClass().getName()));
			}
		}
	}

}
