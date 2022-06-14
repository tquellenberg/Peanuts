package de.tomsplayground.peanuts.client.editors.security;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import de.tomsplayground.peanuts.domain.dividend.Dividend;

public class DeleteDividendEntryHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
		IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
		if (currentSelection instanceof IStructuredSelection) {
			List<Dividend> data = new ArrayList<Dividend>();
			IStructuredSelection sel = (IStructuredSelection) currentSelection;
			if (! sel.isEmpty()) {
				for (@SuppressWarnings("unchecked")
				Iterator<Dividend> iter = sel.iterator(); iter.hasNext();) {
					data.add(iter.next());
				}
				if (activeEditor instanceof SecurityEditor) {
					SecurityEditor se = (SecurityEditor) activeEditor;
					if (se.getSelectedPage() instanceof DividendEditorPart) {
						DividendEditorPart pep = (DividendEditorPart) se.getSelectedPage();
						pep.deleteDividendEntries(data);
					}
				}
			}
		}
		return null;
	}

}
