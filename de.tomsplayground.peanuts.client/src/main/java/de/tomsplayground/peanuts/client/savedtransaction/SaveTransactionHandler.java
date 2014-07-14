package de.tomsplayground.peanuts.client.savedtransaction;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.process.SavedTransaction;
import de.tomsplayground.peanuts.domain.process.Transaction;

public class SaveTransactionHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
		Transaction transaction = (Transaction) ((IStructuredSelection)currentSelection).getFirstElement();
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		InputDialog dialog = new InputDialog(window.getShell(), "Save transaction", "Name", "", new IInputValidator() {
			@Override
			public String isValid(String newText) {
				SavedTransaction savedTransaction = Activator.getDefault().getAccountManager().getSavedTransaction(newText);
				if (savedTransaction != null) {
					return "Saved transaction with this name already exist.";
				}
				if (StringUtils.isBlank(newText)) {
					return "Name must no be empty";
				}
				return null;
			}
		});
		if (dialog.open() == Window.OK) {
			SavedTransaction savedTransaction = new SavedTransaction(dialog.getValue().trim(), transaction);
			Activator.getDefault().getAccountManager().addSavedTransaction(savedTransaction);
		}
		return null;
	}

}
