package de.tomsplayground.peanuts.client.quicken;

import java.io.File;
import java.io.IOException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchWindow;

import de.tomsplayground.peanuts.client.ICommandIds;


public class QifImportAction extends Action {

	private final IWorkbenchWindow window;

	public QifImportAction(IWorkbenchWindow window) {
		this.window = window;
		// The id is used to refer to the action in a menu or toolbar
		setId(ICommandIds.CMD_IMPORT_QIF);
		// Associate the action with a pre-defined command, to allow key bindings.
		setActionDefinitionId(ICommandIds.CMD_IMPORT_QIF);
	}

	@Override
	public void run() {
		FileDialog openDialog = new FileDialog(window.getShell(), SWT.OPEN);
		openDialog.setFilterExtensions(new String[] { "QIF" });
		String filename = openDialog.open();
		try {
			File file = new File(filename);
			if ( !file.isFile() | !file.canRead() | !file.getName().endsWith("QIF")) {
				throw new IOException("File not okay");
			}

			Dialog dialog = new WizardDialog(window.getShell(), new QifWizard(file));
			dialog.open();
		} catch (RuntimeException e) {
			e.printStackTrace();
			MessageDialog.openError(window.getShell(), "Error", e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			MessageDialog.openError(window.getShell(), "Error", e.getMessage());
		}
	}

}
