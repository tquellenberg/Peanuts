package de.tomsplayground.peanuts.client.watchlist;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class SetWatchlistPerformanceRange extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		WatchlistManager watchlistManager = WatchlistManager.getInstance();
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		DateFilterDialog dialog = new DateFilterDialog(window.getShell());
		dialog.setStartDay(watchlistManager.getPerformanceFrom());
		dialog.setEndDay(watchlistManager.getPerformanceTo());
		if (dialog.open() == Window.OK) {
			watchlistManager.setPerformanceFrom(dialog.getStartDay());
			watchlistManager.setPerformanceTo(dialog.getEndDay());
		}
		return null;
	}
	
}
