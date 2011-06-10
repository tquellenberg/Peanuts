package de.tomsplayground.peanuts.client.watchlist;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;


public class AddWatchlistHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		InputDialog dialog = new InputDialog(window.getShell(), "Add watch list", "Name", "", new IInputValidator() {
			@Override
			public String isValid(String newText) {
				List<String> existingNames = WatchlistManager.getInstance().getWatchlistNames();
				if (existingNames.contains(newText.trim())) {
					return "Watch list with this name already exist.";
				}
				if (StringUtils.isBlank(newText)) {
					return "Name must no be empty";
				}
				return null;
			}
		});
		if (dialog.open() == Window.OK) {
			WatchlistManager.getInstance().addWatchlist(dialog.getValue().trim());
		}
		return null;
	}
}
