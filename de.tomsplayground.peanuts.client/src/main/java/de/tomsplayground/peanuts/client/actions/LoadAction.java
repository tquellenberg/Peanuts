package de.tomsplayground.peanuts.client.actions;

import java.io.IOException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchWindow;

import de.tomsplayground.peanuts.client.ICommandIds;
import de.tomsplayground.peanuts.client.app.Activator;


public class LoadAction extends Action {

	private IWorkbenchWindow window;

	public LoadAction(IWorkbenchWindow window) {
		super();
		setId(ICommandIds.CMD_LOAD_BPX);
		setActionDefinitionId(ICommandIds.CMD_LOAD_BPX);
		this.window = window;
	}

	@Override
	public void run() {
		FileDialog openDialog = new FileDialog(window.getShell(), SWT.OPEN);
		openDialog.setFilterExtensions(new String[] { "BPX" });
		String filename = openDialog.open();
		if (filename != null) {
			try {
				Activator.getDefault().load(filename);
			} catch (IOException e) {
				MessageDialog.openError(window.getShell(), "Error", e.getMessage());
			}
		}
	}
}
