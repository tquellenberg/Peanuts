package de.tomsplayground.peanuts.client.editors.account;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;

import de.tomsplayground.peanuts.client.util.EditorInputPropertyDialogAction;

public class AccountEditorActionBarContributor extends MultiPageEditorActionBarContributor {

	private CsvTransactionImportAction csvImportAction;
	private IEditorPart editorPart;

	@Override
	public void contributeToMenu(IMenuManager menuManager) {
		csvImportAction = new CsvTransactionImportAction();
		csvImportAction.setText("CSV Import");
		IMenuManager fileMenu = (IMenuManager) menuManager.find(IWorkbenchActionConstants.M_FILE);
		fileMenu.insertAfter(IWorkbenchActionConstants.MB_ADDITIONS, csvImportAction);
	}
	
	@Override
	public void setActivePage(IEditorPart activeEditor) {
		// ignore page
	}
	
	@Override
	public void setActiveEditor(IEditorPart part) {
		this.editorPart = part;
		csvImportAction.setEditorPart(editorPart);
		super.setActiveEditor(part);
		IActionBars actionBars = part.getEditorSite().getActionBars();
		actionBars.setGlobalActionHandler(ActionFactory.PROPERTIES.getId(), 
			new EditorInputPropertyDialogAction(part.getSite(), part.getEditorInput()));
	}

}
