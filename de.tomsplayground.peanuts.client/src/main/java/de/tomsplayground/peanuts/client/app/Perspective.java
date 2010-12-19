package de.tomsplayground.peanuts.client.app;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import de.tomsplayground.peanuts.client.views.NavigationView;
import de.tomsplayground.peanuts.client.wizards.security.SecurityNewWizard;


public class Perspective implements IPerspectiveFactory {

	@Override
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);
		layout.addView(NavigationView.ID, IPageLayout.LEFT, 0.25f, editorArea);
		layout.addNewWizardShortcut(SecurityNewWizard.ID);
	}
}
