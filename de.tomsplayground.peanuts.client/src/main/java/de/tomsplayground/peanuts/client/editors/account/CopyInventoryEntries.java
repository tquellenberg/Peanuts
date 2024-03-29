package de.tomsplayground.peanuts.client.editors.account;

import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

import de.tomsplayground.peanuts.domain.base.InventoryEntry;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class CopyInventoryEntries extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
		Clipboard cb = new Clipboard(Display.getDefault());
		String content = "";
		if (currentSelection instanceof IStructuredSelection structuredSelection) {
			@SuppressWarnings("rawtypes")
			Iterator iterator = structuredSelection.iterator();
			while (iterator.hasNext()) {
				InventoryEntry entry = (InventoryEntry) iterator.next();
				content += entry.getSecurity().getName()
					+ "\t" + entry.getSecurity().getISIN()
					+ "\t" + PeanutsUtil.formatCurrency(entry.getInvestedAmount(), null)
					+ "\t" + PeanutsUtil.formatCurrency(entry.getMarketValue(), null)
					+ System.lineSeparator();
			}
		}
		if (StringUtils.isNoneEmpty(content)) {
			TextTransfer textTransfer = TextTransfer.getInstance();
			cb.setContents(new Object[] { content }, new Transfer[] { textTransfer });
		}
		return null;
	}

}
