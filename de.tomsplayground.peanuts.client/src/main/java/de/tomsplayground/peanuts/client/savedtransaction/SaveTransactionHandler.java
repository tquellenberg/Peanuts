package de.tomsplayground.peanuts.client.savedtransaction;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.process.SavedTransaction;
import de.tomsplayground.peanuts.domain.process.Transaction;

public class SaveTransactionHandler extends AbstractHandler {

	private final static Logger log = LoggerFactory.getLogger(SaveTransactionHandler.class);

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
		Transaction transaction = (Transaction) ((IStructuredSelection)currentSelection).getFirstElement();
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		InputDialog dialog = new InputDialog(window.getShell(), "Save transaction", "Name", "", t -> validName(t));
		if (dialog.open() == Window.OK) {
			String name = dialog.getValue().trim();
			SavedTransaction savedTransaction = Activator.getDefault().getAccountManager().getSavedTransaction(name);
			if (savedTransaction != null) {
				// Update
				log.info("Update {}", transaction);
				savedTransaction.setTransaction(transaction);
			} else {
				// New
				log.info("New {}", transaction);
				SavedTransaction newSavedTransaction = new SavedTransaction(name, transaction);
				Activator.getDefault().getAccountManager().addSavedTransaction(newSavedTransaction);
			}
		}
		return null;
	}

	private String validName(String newText) {
		if (StringUtils.isBlank(newText)) {
			return "Name must no be empty";
		}
		return null;
	}
}
