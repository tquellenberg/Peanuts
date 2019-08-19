package de.tomsplayground.peanuts.client.app;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
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
		addField(new DirectoryFieldEditor(Activator.SECURITYPRICEPATH_PROPERTY, "Directory with securty prices", parent));
		addField(new StringFieldEditor(Activator.RAPIDAPIKEY_PROPERTY, "RapidAPI Key", parent));
	}

	@Override
	public void init(IWorkbench workbench) {
		// nothing to do
	}

}
