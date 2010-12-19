package de.tomsplayground.peanuts.client.util;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.dialogs.PropertyDialogAction;


public class EditorInputPropertyDialogAction extends PropertyDialogAction {

	public EditorInputPropertyDialogAction(IShellProvider shell, final IEditorInput editorInput) {
		super(shell, new ISelectionProvider() {
			@Override
			public void addSelectionChangedListener(ISelectionChangedListener listener) {
				// unsupported
			}
			@Override
			public ISelection getSelection() {
				return new StructuredSelection(editorInput);
			}
			@Override
			public void removeSelectionChangedListener(ISelectionChangedListener listener) {
				// unsupported
			}
			@Override
			public void setSelection(ISelection selection) {
				// unsupported
			}
		});
	}

}
