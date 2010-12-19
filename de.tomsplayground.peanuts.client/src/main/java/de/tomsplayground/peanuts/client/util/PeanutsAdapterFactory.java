package de.tomsplayground.peanuts.client.util;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.INamedElement;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.Credit;
import de.tomsplayground.peanuts.domain.process.ICredit;
import de.tomsplayground.peanuts.domain.reporting.forecast.Forecast;
import de.tomsplayground.peanuts.domain.reporting.transaction.Report;
import de.tomsplayground.peanuts.domain.statistics.SecurityCategoryMapping;

public class PeanutsAdapterFactory implements IAdapterFactory {

	private Object securityWorkbenchAdapter = new IWorkbenchAdapter() {

		@Override
		public Object[] getChildren(Object o) {
			return null;
		}

		@Override
		public ImageDescriptor getImageDescriptor(Object obj) {
			if (obj instanceof Account) {
				return Activator.getDefault().getImageRegistry().getDescriptor(Activator.IMAGE_ACCOUNT);
			} else if (obj instanceof Report) {
				return Activator.getDefault().getImageRegistry().getDescriptor(Activator.IMAGE_REPORT);
			} else if (obj instanceof Forecast) {
				return Activator.getDefault().getImageRegistry().getDescriptor(Activator.IMAGE_FORECAST);
			} else if (obj instanceof ICredit) {
				return Activator.getDefault().getImageRegistry().getDescriptor(Activator.IMAGE_CREDIT);
			} else if (obj instanceof Security) {
				return Activator.getDefault().getImageRegistry().getDescriptor(Activator.IMAGE_SECURITY);
			} else if (obj instanceof SecurityCategoryMapping) {
				return Activator.getDefault().getImageRegistry().getDescriptor(Activator.IMAGE_SECURITYCATEGORY);
			}
			return null;
		}

		@Override
		public String getLabel(Object o) {
			return ((INamedElement) o).getName();
		}

		@Override
		public Object getParent(Object o) {
			return null;
		}		
	};

	@Override
	public Object getAdapter(Object adaptableObject, @SuppressWarnings("rawtypes") Class adapterType) {
		if (adapterType == IWorkbenchAdapter.class) {
			if (adaptableObject instanceof INamedElement) {
				return securityWorkbenchAdapter;
			}
		} else if ((adapterType == Security.class || adapterType == Credit.class)
			&& adaptableObject instanceof IAdaptable) {
			return ((IAdaptable) adaptableObject).getAdapter(adapterType);
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class[] getAdapterList() {
		return new Class[] {
    		IWorkbenchAdapter.class,
    		Security.class,
    		Credit.class
    	};
	}

}
