package de.tomsplayground.peanuts.client.editors.credit;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

import de.tomsplayground.peanuts.domain.process.Credit;

public class CreditEditorInput implements IEditorInput {

	private Credit credit;

	public CreditEditorInput(Credit credit) {
		this.credit = credit;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public boolean equals(Object obj) {
		if ( !getClass().equals(obj.getClass())) {
			return false;
		}
		return credit.equals(((CreditEditorInput) obj).credit);
	}

	@Override
	public int hashCode() {
		return credit.hashCode();
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageDescriptor.getMissingImageDescriptor();
	}

	@Override
	public String getName() {
		return credit.getName();
	}

	@Override
	public IPersistableElement getPersistable() {
		return new IPersistableElement() {

			@Override
			public String getFactoryId() {
				return CreditEditorFactory.ID;
			}

			@Override
			public void saveState(IMemento memento) {
				memento.putString(CreditEditorFactory.CREDIT_NAME, credit.getName());
			}
		};
	}

	@Override
	public String getToolTipText() {
		return credit.getName();
	}

	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		if (adapter == Credit.class) {
			return credit;
		}
		return null;
	}

	public Credit getCredit() {
		return credit;
	}

}
