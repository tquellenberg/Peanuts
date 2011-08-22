package de.tomsplayground.peanuts.client.savedtransaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.process.SavedTransaction;

public class InsertSavedTransactionItems extends CompoundContributionItem {

	public static final String ID = "de.tomsplayground.peanuts.client.inserTransactionItems";
	public static final String COMMAND_ID = "de.tomsplayground.peanuts.client.insertTransaction.cmd";
	public static final String SAVED_TRANSACTION_NAME = "de.tomsplayground.peanuts.client.insertSavedTransaction.name";

	@Override
	protected IContributionItem[] getContributionItems() {
		List<CommandContributionItem> result = new ArrayList<CommandContributionItem>();
		ImmutableList<SavedTransaction> savedTransactions = Activator.getDefault().getAccountManager().getSavedTransactions();
		for (SavedTransaction savedTransaction : savedTransactions) {
			CommandContributionItemParameter contributionParameters = new CommandContributionItemParameter(
					PlatformUI.getWorkbench().getActiveWorkbenchWindow(), ID, COMMAND_ID, CommandContributionItem.STYLE_PUSH);
			contributionParameters.label = savedTransaction.getName();
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(SAVED_TRANSACTION_NAME, savedTransaction.getName());
			contributionParameters.parameters = parameters;
			CommandContributionItem contributionItem = new CommandContributionItem(contributionParameters);
			result.add(contributionItem);
		}
		return result.toArray(new CommandContributionItem[result.size()]);
	}

}
