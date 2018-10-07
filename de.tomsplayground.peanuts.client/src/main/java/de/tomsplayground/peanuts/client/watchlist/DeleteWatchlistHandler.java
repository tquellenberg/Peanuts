package de.tomsplayground.peanuts.client.watchlist;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.HandlerEvent;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.handlers.HandlerUtil;

public class DeleteWatchlistHandler extends AbstractHandler {

	public DeleteWatchlistHandler() {
		WatchlistManager watchlistManager = WatchlistManager.getInstance();
		watchlistManager.addPropertyChangeListener("currentWatchlist", e -> {
			// Funktioniert nicht. Keine Ahnung....
			fireHandlerChanged(new HandlerEvent(this, true, false));
		});
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		WatchlistManager watchlistManager = WatchlistManager.getInstance();
		Watchlist currentWatchlist = watchlistManager.getCurrentWatchlist();
		if (currentWatchlist != null) {
			if (MessageDialog.openConfirm(HandlerUtil.getActiveShell(event), "Delete Watchlist", "Delete watchlist '"+currentWatchlist.getName()+"'?")) {
				watchlistManager.deleteCurrentWatchlist();
			}
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		WatchlistManager watchlistManager = WatchlistManager.getInstance();
		Watchlist currentWatchlist = watchlistManager.getCurrentWatchlist();
		boolean enabled = currentWatchlist != null && ! currentWatchlist.getName().equals(WatchlistManager.DEFAULT_WATCHLIST_NAME)
			&& super.isEnabled();
		return enabled;
	}

}
