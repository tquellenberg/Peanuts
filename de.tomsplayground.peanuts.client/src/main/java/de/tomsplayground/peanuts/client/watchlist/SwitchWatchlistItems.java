package de.tomsplayground.peanuts.client.watchlist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

public class SwitchWatchlistItems extends CompoundContributionItem {

	public static final String ID = "de.tomsplayground.peanuts.client.switchWatchlist";
	public static final String COMMAND_ID = "de.tomsplayground.peanuts.client.switchWatchlist.cmd";
	public static final String WATCHLIST_NAME = "de.tomsplayground.peanuts.client.switchWatchlist.watchListName";

	public SwitchWatchlistItems() {
		super();
	}
	
	public SwitchWatchlistItems(String id) {
		super(id);
	}

	@Override
	protected IContributionItem[] getContributionItems() {
		List<CommandContributionItem> result = new ArrayList<CommandContributionItem>();
		List<String> watchlistNames = WatchlistManager.getInstance().getWatchlistNames();
		for (String name : watchlistNames) {
			CommandContributionItemParameter contributionParameters = new CommandContributionItemParameter(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), ID, COMMAND_ID, CommandContributionItem.STYLE_RADIO);
			contributionParameters.label = name;
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(WATCHLIST_NAME, name);
			contributionParameters.parameters = parameters;
			CommandContributionItem contributionItem = new CommandContributionItem(contributionParameters);
			result.add(contributionItem);
		}
		return result.toArray(new CommandContributionItem[result.size()]);
	}
}
