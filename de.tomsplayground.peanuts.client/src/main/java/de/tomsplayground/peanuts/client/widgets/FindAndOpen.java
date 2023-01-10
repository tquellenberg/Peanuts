package de.tomsplayground.peanuts.client.widgets;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredList;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.INamedElement;

public class FindAndOpen {

	public void openDialog(Display display, Shell parentShell) {
		final Shell dialog = new Shell(parentShell, SWT.DIALOG_TRIM);
		dialog.setLayout(new GridLayout(1, false));

		ILabelProvider labelProvider = new ColumnLabelProvider();
		FilteredList filteredList = new FilteredList(dialog, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER, labelProvider, true, false, false);
		filteredList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		filteredList.setElements(getElements());
		
		dialog.setSize(300, 400);
		Monitor primary = display.getPrimaryMonitor();
		Rectangle bounds = primary.getBounds();
		Rectangle rect = dialog.getBounds();
		int x = bounds.x + (bounds.width - rect.width) / 2;
		int y = bounds.y + (bounds.height - rect.height) / 2;
		dialog.setLocation(x, y);
		dialog.open();
	}
	
	private INamedElement[] getElements() {
		AccountManager accountManager = Activator.getDefault().getAccountManager();
		return accountManager.getSecurities().toArray(new INamedElement[0]);
	}

}
