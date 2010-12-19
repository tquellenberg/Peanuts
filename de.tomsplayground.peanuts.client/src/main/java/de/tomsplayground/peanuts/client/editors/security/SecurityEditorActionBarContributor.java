package de.tomsplayground.peanuts.client.editors.security;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;

import de.tomsplayground.peanuts.client.util.EditorInputPropertyDialogAction;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;

public class SecurityEditorActionBarContributor extends MultiPageEditorActionBarContributor {

	@Override
	public void setActivePage(IEditorPart activeEditor) {
		// ignore page
	}

	@Override
	public void setActiveEditor(final IEditorPart part) {
		super.setActiveEditor(part);
		IActionBars actionBars = part.getEditorSite().getActionBars();
		actionBars.setGlobalActionHandler(ActionFactory.REFRESH.getId(), new Action("Refresh") {
			@Override
			public void run() {
				Security security = ((SecurityEditorInput) part.getEditorInput()).getSecurity();
				PriceProviderFactory.getInstance().refresh(security);
			}
		});
			
		actionBars.setGlobalActionHandler(ActionFactory.PROPERTIES.getId(), 
			new EditorInputPropertyDialogAction(part.getSite(), part.getEditorInput()));
	}

}
