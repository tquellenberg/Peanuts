package de.tomsplayground.peanuts.client.navigation;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.reporting.transaction.Report;

public class DeleteReportFromNavigation extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
		if (currentSelection instanceof IStructuredSelection struturedSel) {
			@SuppressWarnings("rawtypes")
			Iterator iterator = struturedSel.iterator();
			while (iterator.hasNext()) {
				Report entry = (Report)iterator.next();
				Activator.getDefault().getAccountManager().removeReport(entry);
			}
		}
		return null;
	}

}
