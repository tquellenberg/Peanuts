package de.tomsplayground.peanuts.client.savedtransaction;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.editors.account.AccountEditorInput;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.process.SavedTransaction;
import de.tomsplayground.peanuts.domain.process.Transaction;
import de.tomsplayground.util.Day;

public class InsertTransactionHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String savedTransactionName = event.getParameter(InsertSavedTransactionItems.SAVED_TRANSACTION_NAME);
		SavedTransaction savedTransaction = Activator.getDefault().getAccountManager().getSavedTransaction(savedTransactionName);

		IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
		AccountEditorInput editorInput = (AccountEditorInput) activeEditor.getEditorInput();
		Account account = editorInput.getAccount();

		Transaction transaction = (Transaction) savedTransaction.getTransaction().clone();
		transaction.setDay(new Day());
		account.addTransaction(transaction);
		return null;
	}

}
