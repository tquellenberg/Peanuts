package de.tomsplayground.peanuts.client.editors.securitycategory;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;

public class SecurityCategoryEditor extends MultiPageEditorPart {

	public static final String ID = "de.tomsplayground.peanuts.client.securityCategoryEditor";

	private final List<IEditorPart> editors = new ArrayList<IEditorPart>();

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if ( !(input instanceof SecurityCategoryEditorInput)) {
			throw new PartInitException("Invalid Input: Must be SecurityCategoryEditorInput");
		}
		super.init(site, input);
		setPartName(input.getName());

		addPageChangedListener(new IPageChangedListener() {
			@Override
			public void pageChanged(PageChangedEvent event) {
				if (event.getSelectedPage() instanceof PieEditorPart) {
					PieEditorPart pieEditorPart = (PieEditorPart) event.getSelectedPage();
					pieEditorPart.updateDataset();
				}
			}
		});
	}

	@Override
	protected void createPages() {
		createEditorPage(new PieEditorPart(), "Pie");
		createEditorPage(new DetailPart(), "Details");
	}

	private void createEditorPage(IEditorPart editor, String name) {
		try {
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
}
