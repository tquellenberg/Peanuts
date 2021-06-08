package de.tomsplayground.peanuts.client.actions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.google.common.collect.ImmutableSet;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.base.InventoryEntry;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.dividend.Dividend;
import de.tomsplayground.peanuts.util.Day;

public class CopyDividendsToNewYear extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job copyDividendsJob = new Job("Copy dividends") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				int year = Day.today().year;
				try {
					AccountManager accountManager = Activator.getDefault().getAccountManager();
					Inventory fullInventory = accountManager.getFullInventory();
					ImmutableSet<InventoryEntry> entries = fullInventory.getEntries();
					monitor.beginTask("Copy dividends", entries.size());
					for (InventoryEntry entry : entries) {
						if (entry.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
							Security security = entry.getSecurity();
							monitor.subTask("Copy " + security.getName());
							copyDividends(security, year);
						}
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
		copyDividendsJob.setUser(false);
		copyDividendsJob.setSystem(false);
		copyDividendsJob.schedule();
		return null;
	}

	private void copyDividends(Security security, int year) {
		List<Dividend> allDividends = new ArrayList<>(security.getDividends());

		List<Dividend> newDividends = allDividends.stream()
			.filter(d -> d.getPayDate().year == year)
			.map(d -> copyNextYear(d)).collect(Collectors.toList());

		for (Dividend newDividend : newDividends) {
			if (! allDividends.stream().anyMatch(d -> d.getPayDate().equals(newDividend.getPayDate()))) {
				allDividends.add(newDividend);
			}
		}

		security.updateDividends(allDividends);
	}

	private Dividend copyNextYear(Dividend d) {
		return new Dividend(d.getPayDate().addYear(1), d.getAmountPerShare(), d.getCurrency());
	}

}
