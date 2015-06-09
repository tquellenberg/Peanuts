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

import de.tomsplayground.peanuts.domain.fundamental.FundamentalData;

public class IgnoreFundamentalDataHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
		IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
		if (currentSelection instanceof IStructuredSelection) {
			List<FundamentalData> data = new ArrayList<FundamentalData>();
			@SuppressWarnings("rawtypes")
			Iterator iterator = ((IStructuredSelection) currentSelection).iterator();
			while (iterator.hasNext()) {
				FundamentalData p = (FundamentalData)iterator.next();
				data.add(p);
			}
			if (activeEditor instanceof SecurityEditor) {
				SecurityEditor se = (SecurityEditor)activeEditor;
				if (se.getSelectedPage() instanceof FundamentalDataEditorPart) {
					FundamentalDataEditorPart pep = (FundamentalDataEditorPart)se.getSelectedPage();
					pep.ignoreFundamentalData(data);
				}
			}
		}
		return null;
	}

}
