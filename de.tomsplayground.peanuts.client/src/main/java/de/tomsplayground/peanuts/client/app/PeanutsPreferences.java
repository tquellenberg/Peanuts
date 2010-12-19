package de.tomsplayground.peanuts.client.app;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PeanutsPreferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public PeanutsPreferences() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}

	@Override
	protected void createFieldEditors() {
		Composite parent = getFieldEditorParent();
		addField(new DirectoryFieldEditor("securitypricepath", "Directory with securty prices", parent));
	}

	@Override
	public void init(IWorkbench workbench) {
		// nothing to do
	}

}
