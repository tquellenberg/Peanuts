package de.tomsplayground.peanuts.client.editors.account;

import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.process.Credit;
import de.tomsplayground.peanuts.domain.process.ICredit;

public class AccountEditorInput implements IEditorInput {

	Account account;

	@Override
	public boolean equals(Object obj) {
		if ( !getClass().equals(obj.getClass())) {
			return false;
		}
		return account.equals(((AccountEditorInput) obj).account);
	}

	@Override
	public int hashCode() {
		return account.hashCode();
	}

	public AccountEditorInput(Account account) {
		this.account = account;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageDescriptor.getMissingImageDescriptor();
	}

	@Override
	public String getName() {
		return account.getName();
	}

	@Override
	public IPersistableElement getPersistable() {
		return new IPersistableElement() {

			@Override
			public String getFactoryId() {
				return AccountEditorFactory.ID;
			}

			@Override
			public void saveState(IMemento memento) {
				memento.putString(AccountEditorFactory.ACCOUNT_NAME, account.getName());
			}

		};
	}

	@Override
	public String getToolTipText() {
		return account.getType().toString() + " : " + account.getName();
	}

	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		if (adapter == Account.class) {
			return account;
		} else if (adapter == Credit.class) {
			List<ICredit> credits = Activator.getDefault().getAccountManager().getCredits();
			for (ICredit c : credits) {
				if (c.getConnection().equals(account))
					return c;
			}
		}
		return null;
	}

	public Account getAccount() {
		return account;
	}

}
