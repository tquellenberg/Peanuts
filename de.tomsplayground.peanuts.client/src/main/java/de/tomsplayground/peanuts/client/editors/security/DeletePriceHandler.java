package de.tomsplayground.peanuts.client.editors.security;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;

public class DeletePriceHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
		IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
		Security security = ((SecurityEditorInput) activeEditor.getEditorInput()).getSecurity();
		IPriceProvider priceProvider = PriceProviderFactory.getInstance().getPriceProvider(security);
		if (currentSelection instanceof IStructuredSelection) {
			@SuppressWarnings("rawtypes")
			Iterator iterator = ((IStructuredSelection) currentSelection).iterator();
			while (iterator.hasNext()) {
				Price p = (Price)iterator.next();
				priceProvider.removePrice(p.getDay());
			}
			
			if (activeEditor instanceof SecurityEditor) {
				SecurityEditor se = (SecurityEditor)activeEditor;
				if (se.getSelectedPage() instanceof PriceEditorPart) {
					PriceEditorPart pep = (PriceEditorPart)se.getSelectedPage();
					pep.markDirty();
				}
			}
		}
		return null;
	}
}
