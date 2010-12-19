package de.tomsplayground.peanuts.client.editors.credit;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.process.Credit;

public class CreditEditorFactory implements IElementFactory {

	public static final String ID = "de.tomsplayground.peanuts.client.creditEditorFactoryId";
	public static final String CREDIT_NAME = "credit.name";

	@Override
	public IAdaptable createElement(IMemento memento) {
		String name = memento.getString(CREDIT_NAME);
		AccountManager accountManager = Activator.getDefault().getAccountManager();
		Credit credit = (Credit) accountManager.getCredit(name);
		if (credit == null) {
			credit = new Credit(name);
			accountManager.addCredit(credit);
		}
		return new CreditEditorInput(credit);
	}

}
