package de.tomsplayground.peanuts.client.editors.account;

import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.editors.ITransactionProviderInput;
import de.tomsplayground.peanuts.config.IConfigurable;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.ITransactionProvider;
import de.tomsplayground.peanuts.domain.process.Credit;
import de.tomsplayground.peanuts.domain.process.ICredit;

public class AccountEditorInput implements IEditorInput, ITransactionProviderInput {

	Account account;

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != getClass()) {
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
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == Account.class) {
			return adapter.cast(account);
		} else if (adapter == Credit.class) {
			List<ICredit> credits = Activator.getDefault().getAccountManager().getCredits();
			for (ICredit c : credits) {
				if (c.getConnection().equals(account)) {
					return adapter.cast(c);
				}
			}
		} else if (adapter == IConfigurable.class) {
			return adapter.cast(account);
		}
		return null;
	}

	public Account getAccount() {
		return account;
	}

	@Override
	public ITransactionProvider getTransactionProvider() {
		return account;
	}

}
