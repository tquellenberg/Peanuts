package de.tomsplayground.peanuts.client.editors.security;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

import de.tomsplayground.peanuts.domain.base.Security;

public class SecurityEditorInput implements IEditorInput {

	Security security;

	public SecurityEditorInput(Security security) {
		this.security = security;
	}

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
		return security.equals(((SecurityEditorInput) obj).security);
	}

	@Override
	public int hashCode() {
		return security.hashCode();
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
		return security.getName();
	}

	@Override
	public IPersistableElement getPersistable() {
		return new IPersistableElement() {

			@Override
			public String getFactoryId() {
				return SecurityEditorFactory.ID;
			}

			@Override
			public void saveState(IMemento memento) {
				memento.putString(SecurityEditorFactory.SECURITY_NAME, security.getName());
			}

		};
	}

	@Override
	public String getToolTipText() {
		return security.getName();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		if (adapter.isAssignableFrom(Security.class)) {
			return security;
		}
		return null;
	}

	public Security getSecurity() {
		return security;
	}

}
