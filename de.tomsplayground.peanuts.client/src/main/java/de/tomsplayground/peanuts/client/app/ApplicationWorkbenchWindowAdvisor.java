package de.tomsplayground.peanuts.client.app;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

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
		Job job = new Job("Refresh investment prices") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					PriceProviderFactory priceProviderFactory = PriceProviderFactory.getInstance();
					List<Security> securities = new ArrayList<Security>(Activator.getDefault().getAccountManager().getSecurities());
					monitor.beginTask("Refresh investment prices", securities.size());
					for (Security security : securities) {
						monitor.subTask("Refreshing " + security.getName());
						priceProviderFactory.refresh(security);
						monitor.worked(1);
						if (monitor.isCanceled())
							return Status.CANCEL_STATUS;
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
