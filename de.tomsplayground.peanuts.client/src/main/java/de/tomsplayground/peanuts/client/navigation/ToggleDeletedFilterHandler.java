package de.tomsplayground.peanuts.client.navigation;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.State;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

public class ToggleDeletedFilterHandler extends AbstractHandler {

	private static final String STATE_ID = "de.tomsplayground.peanuts.client.toggleDeletedFilter.state";
	private static final String CMD_ID = "de.tomsplayground.peanuts.client.toggleDeletedFilter.cmd";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		State state = getDeletedFilterState();
		state.setValue(Boolean.valueOf(! ((Boolean)state.getValue()).booleanValue()));
		return null;
	}

	public static State getDeletedFilterState() {
		ICommandService service = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
		Command command = service.getCommand(CMD_ID);
		return command.getState(STATE_ID);
	}
}
