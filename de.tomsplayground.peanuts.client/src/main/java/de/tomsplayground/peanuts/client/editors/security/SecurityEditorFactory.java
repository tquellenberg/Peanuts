package de.tomsplayground.peanuts.client.editors.security;


import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Security;

public class SecurityEditorFactory implements IElementFactory {

	public static final String ID = "de.tomsplayground.peanuts.client.securityEditorFactoryId";
	public static final String SECURITY_NAME = "security.name";

	@Override
	public IAdaptable createElement(IMemento memento) {
		String name = memento.getString(SECURITY_NAME);
		AccountManager accountManager = Activator.getDefault().getAccountManager();
		Security security = accountManager.getOrCreateSecurity(name);
		return new SecurityEditorInput(security);
	}

}
