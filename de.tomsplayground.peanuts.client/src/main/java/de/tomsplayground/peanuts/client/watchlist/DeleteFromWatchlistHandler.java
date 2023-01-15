package de.tomsplayground.peanuts.client.watchlist;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteFromWatchlistHandler extends AbstractHandler {

	private final static Logger log = LoggerFactory.getLogger(DeleteFromWatchlistHandler.class);

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		log.info("DeleteFromWatchlistHandler: {}", event);
		SecurityWatchlistView activePart = (SecurityWatchlistView) HandlerUtil.getActivePart(event);
		ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
		if (currentSelection instanceof IStructuredSelection structuredSel) {
			@SuppressWarnings("rawtypes")
			Iterator iterator = structuredSel.iterator();
			while (iterator.hasNext()) {
				WatchEntry entry = (WatchEntry)iterator.next();
				activePart.removeSecurityFromCurrentWatchlist(entry.getSecurity());
			}
		} else {
			log.info("currentSelection: {}", currentSelection);
		}
		return null;
	}
}
