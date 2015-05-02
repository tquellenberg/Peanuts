package de.tomsplayground.peanuts.client.navigation;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import de.tomsplayground.peanuts.domain.base.IDeletable;

public class DeleteFromNavigationHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
		if (currentSelection instanceof IStructuredSelection) {
			@SuppressWarnings("rawtypes")
			Iterator iterator = ((IStructuredSelection) currentSelection).iterator();
			while (iterator.hasNext()) {
				IDeletable entry = (IDeletable)iterator.next();
				entry.setDeleted(! entry.isDeleted());
			}
		}
		return null;
	}
}
