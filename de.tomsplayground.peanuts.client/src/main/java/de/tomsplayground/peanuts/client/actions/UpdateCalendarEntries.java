package de.tomsplayground.peanuts.client.actions;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.calendar.CalendarUpdateJob;
import de.tomsplayground.peanuts.domain.calendar.SecurityCalendarEntry;

public class UpdateCalendarEntries extends AbstractHandler {

	private final static Logger log = LoggerFactory.getLogger(UpdateCalendarEntries.class);

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job refreshJob = new Job("Add new calendar entries") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					AccountManager accountManager = Activator.getDefault().getAccountManager();
					List<Security> securities = securitiesToUpdate();
					CalendarUpdateJob updateJob = new CalendarUpdateJob();
					String apiKey = Activator.getDefault().getPreferenceStore()
							.getString(Activator.RAPIDAPIKEY_PROPERTY);
					updateJob.setApiKey(apiKey);
					monitor.beginTask("Add new calendar entries", securities.size());
					for (Security security : securities) {
						if (updateJob.isEnabled(security)) {
							monitor.subTask("Checking " + security.getName());

							List<SecurityCalendarEntry> entries = Lists
									.newArrayList(accountManager.getCalendarEntries(security));
							updateJob.findNewCalendarEntry(security, entries).ifPresent(e -> {
								log.info("Add new calendar entry for {}", security.getName());
								accountManager.addCalendarEntry(e);
							});
						}

						monitor.worked(1);
						try {
							Thread.sleep(1000 * 1);
						} catch (InterruptedException e) {
							return Status.CANCEL_STATUS;
						}
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
		refreshJob.setUser(false);
		refreshJob.setSystem(false);
		refreshJob.schedule();
		return null;
	}

	private List<Security> securitiesToUpdate() {
		Inventory fullInventory = Activator.getDefault().getAccountManager().getFullInventory();
		List<Security> securities = fullInventory.getEntries().stream()
				.filter(e -> e.getQuantity().signum() > 0)
				.map(e -> e.getSecurity()).collect(Collectors.toList());
		securities.sort(Comparator.comparing(Security::getName));
		return securities;
	}
}
