package de.tomsplayground.peanuts.client.editors.securitycategory;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

import de.tomsplayground.peanuts.domain.statistics.SecurityCategoryMapping;

public class SecurityCategoryEditorInput implements IEditorInput {

	SecurityCategoryMapping securityCategoryMapping;

	public SecurityCategoryEditorInput(SecurityCategoryMapping securityCategoryMapping) {
		this.securityCategoryMapping = securityCategoryMapping;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		if (adapter.isAssignableFrom(SecurityCategoryMapping.class))
			return securityCategoryMapping;
		return null;
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
		return securityCategoryMapping.getName();
	}

	@Override
	public IPersistableElement getPersistable() {
		return new IPersistableElement() {
			@Override
			public String getFactoryId() {
				return SecurityCategoryEditorFactory.ID;
			}

			@Override
			public void saveState(IMemento memento) {
				memento.putString(SecurityCategoryEditorFactory.SECURITY_CATEGORY_NAME, securityCategoryMapping.getName());
			}
		};
	}

	@Override
	public String getToolTipText() {
		return securityCategoryMapping.getName();
	}

	@Override
	public boolean equals(Object obj) {
		if ( !getClass().equals(obj.getClass())) {
			return false;
		}
		return securityCategoryMapping.equals(((SecurityCategoryEditorInput) obj).securityCategoryMapping);
	}

	@Override
	public int hashCode() {
		return securityCategoryMapping.hashCode();
	}

	public SecurityCategoryMapping getSecurityCategoryMapping() {
		return securityCategoryMapping;
	}
}
