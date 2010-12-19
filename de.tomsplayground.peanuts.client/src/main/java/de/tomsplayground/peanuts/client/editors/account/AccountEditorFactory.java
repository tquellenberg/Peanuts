package de.tomsplayground.peanuts.client.editors.account;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.AccountManager;

public class AccountEditorFactory implements IElementFactory {

	public static final String ID = "de.tomsplayground.peanuts.client.accountEditorFactoryId";
	public static final String ACCOUNT_NAME = "account.name";

	@Override
	public IAdaptable createElement(IMemento memento) {
		String name = memento.getString(ACCOUNT_NAME);
		AccountManager accountManager = Activator.getDefault().getAccountManager();
		Account account = accountManager.getOrCreateAccount(name, Account.Type.UNKNOWN);
		return new AccountEditorInput(account);
	}

}
