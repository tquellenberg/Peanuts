package de.tomsplayground.peanuts.client.editors.securitycategory;


import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.statistics.SecurityCategoryMapping;

public class SecurityCategoryEditorFactory implements IElementFactory {

	public static final String ID = "de.tomsplayground.peanuts.client.securityCategoryEditorFactoryId";
	public static final String SECURITY_CATEGORY_NAME = "securityCategory.name";

	@Override
	public IAdaptable createElement(IMemento memento) {
		String name = memento.getString(SECURITY_CATEGORY_NAME);
		AccountManager accountManager = Activator.getDefault().getAccountManager();
		SecurityCategoryMapping mapping = accountManager.getSecurityCategoryMapping(name);
		return new SecurityCategoryEditorInput(mapping);
	}

}
