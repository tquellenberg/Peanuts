package de.tomsplayground.peanuts.client.watchlist;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

public class DeleteFromWatchlistHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		SecurityWatchlistView activePart = (SecurityWatchlistView) HandlerUtil.getActivePart(event);
		ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
		if (currentSelection instanceof IStructuredSelection) {
			@SuppressWarnings("rawtypes")
			Iterator iterator = ((IStructuredSelection) currentSelection).iterator();
			while (iterator.hasNext()) {
				WatchEntry entry = (WatchEntry)iterator.next();
				activePart.removeSecurityFromCurrentWatchlist(entry.getSecurity());
			}
		}
		return null;
	}
}
