package de.tomsplayground.peanuts.client.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.editors.security.properties.SecurityPropertyPage;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.scraping.Scraping;

public class UpdateAllSecurityPrices extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job refreshPricesJob = new Job("Refresh investment prices") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					PriceProviderFactory priceProviderFactory = PriceProviderFactory.getInstance();
					ImmutableList<Security> securities = Activator.getDefault().getAccountManager().getSecurities();
					monitor.beginTask("Refresh investment prices", securities.size());
					for (Security security : securities) {
						if (! security.isDeleted()) {
							monitor.subTask("Refreshing " + security.getName());
							priceProviderFactory.refresh(security, Boolean.valueOf(
								security.getConfigurationValue(SecurityPropertyPage.OVERRIDE_EXISTING_PRICE_DATA)).booleanValue());
							Scraping scraping = new Scraping(security);
							Price price = scraping.execute();
							if (price != null) {
								IPriceProvider priceProvider = priceProviderFactory.getPriceProvider(security);
								priceProvider.setPrice(price);
								priceProviderFactory.saveToLocal(security, priceProvider);
							}
						}
						monitor.worked(1);
						if (monitor.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
						throttle();
					}
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}

			private void throttle() {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		};
		refreshPricesJob.setUser(false);
		refreshPricesJob.setSystem(false);
		refreshPricesJob.schedule();
		return null;
	}

}
