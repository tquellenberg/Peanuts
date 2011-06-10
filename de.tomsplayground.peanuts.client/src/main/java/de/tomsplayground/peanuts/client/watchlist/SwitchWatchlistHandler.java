package de.tomsplayground.peanuts.client.watchlist;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

public class SwitchWatchlistHandler extends AbstractHandler implements IElementUpdater {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String watchlistName = event.getParameter(SwitchWatchlistItems.WATCHLIST_NAME);
		WatchlistManager.getInstance().setCurrentWatchlist(watchlistName);
		return null;
	}
	
	@Override
	public void updateElement(UIElement element, @SuppressWarnings("rawtypes") Map parameters) {
		String watchlistName = (String) parameters.get(SwitchWatchlistItems.WATCHLIST_NAME);
		element.setChecked(WatchlistManager.getInstance().getCurrentWatchlist().getName().equals(watchlistName));
	}
}
