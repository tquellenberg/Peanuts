package de.tomsplayground.peanuts.client.editors.account;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;

public class AccountEditorActionBarContributor extends MultiPageEditorActionBarContributor {

	@Override
	public void contributeToMenu(IMenuManager menuManager) {
	}

	@Override
	public void setActivePage(IEditorPart activeEditor) {
		// ignore page
	}

	@Override
	public void setActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);
		IActionBars actionBars = part.getEditorSite().getActionBars();
		actionBars.setGlobalActionHandler(ActionFactory.PROPERTIES.getId(),
			new PropertyDialogAction(part.getSite(), part.getSite().getSelectionProvider()));

	}

}
