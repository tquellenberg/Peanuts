package de.tomsplayground.peanuts.client.comparison;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.comparision.Comparison;

public class ComparisonEditorFactory implements IElementFactory {

	protected static final String ID = "de.tomsplayground.peanuts.client.comparisionEditorFactoryId";
	protected static final String COMPARISON_NAME = "comparison.name";

	@Override
	public IAdaptable createElement(IMemento memento) {
		String name = memento.getString(COMPARISON_NAME);
		AccountManager accountManager = Activator.getDefault().getAccountManager();
		Comparison comparison = accountManager.getComparisons().stream()
			.filter(c -> c.getName().equals(name))
			.findFirst().get();
		return new ComparisonInput(comparison);
	}

}
