package de.tomsplayground.peanuts.client.app;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.client.editors.security.properties.SecurityPropertyPage;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

	public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		super(configurer);
	}

	@Override
	public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
		return new ApplicationActionBarAdvisor(configurer);
	}

	@Override
	public void preWindowOpen() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setInitialSize(new Point(900, 700));
		configurer.setShowCoolBar(true);
		configurer.setShowStatusLine(true);
		configurer.setShowProgressIndicator(true);
	}

	@Override
	public void postWindowOpen() {
		final IStatusLineManager statusline = getWindowConfigurer().getActionBarConfigurer().getStatusLineManager();
		statusline.add(new ContributionItem() {
			@Override
			public void fill(Composite parent) {
				Label label = new Label(parent, SWT.NONE);
			    label.setText(Activator.getDefault().getFilename());
			}
		});
		Activator.getDefault().getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {			
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().equals(Activator.FILENAME_PROPERTY)) {
					statusline.update(true);
				}
			}
		});
		statusline.update(true);
		
		Job job = new Job("Refresh investment prices") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					PriceProviderFactory priceProviderFactory = PriceProviderFactory.getInstance();
					ImmutableList<Security> securities = Activator.getDefault().getAccountManager().getSecurities();
					monitor.beginTask("Refresh investment prices", securities.size());
					for (Security security : securities) {
						monitor.subTask("Refreshing " + security.getName());
						priceProviderFactory.refresh(security, Boolean.valueOf(
							security.getConfigurationValue(SecurityPropertyPage.OVERRIDE_EXISTING_PRICE_DATA)).booleanValue());
						monitor.worked(1);
						if (monitor.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
					}
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(false);
		job.setSystem(false);
		job.schedule();
	}
}
