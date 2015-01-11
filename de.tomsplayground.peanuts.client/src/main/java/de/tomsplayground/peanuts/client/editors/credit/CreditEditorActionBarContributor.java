package de.tomsplayground.peanuts.client.editors.credit;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;

import de.tomsplayground.peanuts.client.util.EditorInputPropertyDialogAction;

public class CreditEditorActionBarContributor extends MultiPageEditorActionBarContributor {

	@Override
	public void setActivePage(IEditorPart activeEditor) {
		// ignore page
	}

	@Override
	public void setActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);
		IActionBars actionBars = part.getEditorSite().getActionBars();
		actionBars.setGlobalActionHandler(ActionFactory.PROPERTIES.getId(),
			new EditorInputPropertyDialogAction(part.getSite(), part.getEditorInput()));
	}

}
