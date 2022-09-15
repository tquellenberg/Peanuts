package de.tomsplayground.peanuts.client.util;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.model.IWorkbenchAdapter;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.watchlist.WatchEntry;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.INamedElement;
import de.tomsplayground.peanuts.domain.base.InventoryEntry;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.comparision.Comparison;
import de.tomsplayground.peanuts.domain.process.Credit;
import de.tomsplayground.peanuts.domain.process.ICredit;
import de.tomsplayground.peanuts.domain.process.SavedTransaction;
import de.tomsplayground.peanuts.domain.reporting.forecast.Forecast;
import de.tomsplayground.peanuts.domain.reporting.transaction.Report;
import de.tomsplayground.peanuts.domain.statistics.SecurityCategoryMapping;

public class PeanutsAdapterFactory implements IAdapterFactory {

	private final Object securityWorkbenchAdapter = new IWorkbenchAdapter() {

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
			} else if (obj instanceof SavedTransaction) {
				return Activator.getDefault().getImageRegistry().getDescriptor(Activator.IMAGE_SAVED_TRANSACTION);
			} else if (obj instanceof Comparison) {
				return Activator.getDefault().getImageRegistry().getDescriptor(Activator.IMAGE_COMPARISON);
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
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adapterType == IWorkbenchAdapter.class) {
			if (adaptableObject instanceof INamedElement) {
				return adapterType.cast(securityWorkbenchAdapter);
			}
		} else if ((adapterType == Security.class || adapterType == Credit.class)
			&& adaptableObject instanceof IAdaptable) {
			return ((IAdaptable) adaptableObject).getAdapter(adapterType);
		} else if ((adapterType == Security.class) && adaptableObject instanceof InventoryEntry) {
			return adapterType.cast(((InventoryEntry)adaptableObject).getSecurity());
		} else if ((adapterType == Security.class) && adaptableObject instanceof WatchEntry) {
			return adapterType.cast(((WatchEntry)adaptableObject).getSecurity());
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[] {
			IWorkbenchAdapter.class,
			Security.class,
			Credit.class
		};
	}

	public static Class<?>[] getAdaptableClasses() {
		return new Class[] {
			INamedElement.class,
			IEditorInput.class,
			InventoryEntry.class,
			WatchEntry.class
		};
	}
}
