package de.tomsplayground.peanuts.client.watchlist;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import de.tomsplayground.peanuts.domain.watchlist.WatchlistConfiguration;

public class EditWatchListConfigurationHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		WatchlistManager watchlistManager = WatchlistManager.getInstance();
		WatchlistConfiguration watchlistConfiguration = watchlistManager.getCurrentWatchlistConfiguration();
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		WatchlistConfigurationDialog dialog = new WatchlistConfigurationDialog(window.getShell(), watchlistConfiguration);
		if (dialog.open() == Window.OK) {
			watchlistManager.updateCurrentWatchlist(watchlistConfiguration);
		}
		return null;
	}

}
