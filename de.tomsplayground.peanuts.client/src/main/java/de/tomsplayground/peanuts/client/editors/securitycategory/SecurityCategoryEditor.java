package de.tomsplayground.peanuts.client.editors.securitycategory;

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

public class SecurityCategoryEditor extends MultiPageEditorPart implements IPersistableEditor {

	public static final String ID = "de.tomsplayground.peanuts.client.securityCategoryEditor";

	private IMemento memento;
	private final List<IEditorPart> editors = new ArrayList<IEditorPart>();

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if ( !(input instanceof SecurityCategoryEditorInput)) {
			throw new PartInitException("Invalid Input: Must be SecurityCategoryEditorInput");
		}
		super.init(site, input);
		setPartName(input.getName());
	}

	@Override
	protected void createPages() {
		createEditorPage(new PieEditorPart(), "Pie");
		createEditorPage(new DetailPart(), "Details");
	}

	private void createEditorPage(IEditorPart editor, String name) {
		try {
			if (editor instanceof IPersistableEditor) {
				IPersistableEditor pEditor = (IPersistableEditor) editor;
				if (memento != null) {
					IMemento[] childMemento = memento.getChildren(editor.getClass().getName());
					if (childMemento != null) {
						for (IMemento iMemento : childMemento) {
							if (iMemento.getID().equals(getEditorInput().getName())) {
								pEditor.restoreState(iMemento);
							}
						}
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
		// nothing to do
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
		this.memento = memento;
	}

	@Override
	public void saveState(IMemento memento) {
		for (IEditorPart editor : editors) {
			if (editor instanceof IPersistableEditor) {
				IPersistableEditor pEditor = (IPersistableEditor) editor;
				pEditor.saveState(memento.createChild(editor.getClass().getName(), getEditorInput().getName()));
			}
		}
	}

}
