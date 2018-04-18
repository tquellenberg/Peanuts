package de.tomsplayground.peanuts.client.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.joda.time.DateTime;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.app.fourtraders.FourTraders;
import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.fundamental.FundamentalData;
import de.tomsplayground.peanuts.domain.fundamental.FundamentalDatas;

public class Update4TraderFundamentalData extends AbstractHandler {

	private final static int MAX_SECURITY_UPDATED_PER_RUN = 10;

	private final static int MAX_AGE_BEFORE_UPDATE = 28;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job refreshJob = new Job("Refresh fundamental data") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					ImmutableList<Security> securities = Activator.getDefault().getAccountManager().getSecurities();
					monitor.beginTask("Refresh fundamental data", securities.size());
					int updateCount = 0;
					for (Security security : securities) {
						if (isUpdateRequired(security)) {
							monitor.subTask("Refreshing " + security.getName());
							FourTraders fourTraders = new FourTraders();
							String financialsUrl = security.getConfigurationValue("fourTrasdersUrl");
							if (StringUtils.equals(financialsUrl, "-")) {
								continue;
							}
							if (StringUtils.isBlank(financialsUrl)) {
								financialsUrl = fourTraders.scrapFinancialsUrl(security.getISIN());
								security.putConfigurationValue("fourTrasdersUrl", financialsUrl);
							}
							if (StringUtils.isNotBlank(financialsUrl)) {
								updateFundamentaData(security, fourTraders.scrapFinancials(financialsUrl));
								try {
									Thread.sleep(1000*10);
								} catch (InterruptedException e) {
									return Status.CANCEL_STATUS;
								}
								updateCount++;
								if (updateCount >= MAX_SECURITY_UPDATED_PER_RUN) {
									return Status.OK_STATUS;
								}
							}
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
		refreshJob.setUser(false);
		refreshJob.setSystem(false);
		refreshJob.schedule();
		return null;
	}

	private boolean isUpdateRequired(Security security) {
		if (security.isDeleted()) {
			return false;
		}
		FundamentalDatas fundamentalDatas = security.getFundamentalDatas();
		if (fundamentalDatas.isEmpty()) {
			return false;
		}
		Optional<DateTime> maxDate = fundamentalDatas.getMaxModificationDate();
		DateTime d = DateTime.now().minusDays(MAX_AGE_BEFORE_UPDATE);
		if (!maxDate.isPresent() || maxDate.get().compareTo(d) < 0) {
			return true;
		}
		return false;
	}

	private void updateFundamentaData(Security security, List<FundamentalData> newDatas) {
		System.out.println("Updating fundamenta data for " + security.getName());
		List<FundamentalData> fundamentalDatas = new ArrayList<>(security.getFundamentalDatas().getDatas());
		for (FundamentalData newData : newDatas) {
			if (newData.getDividende().signum() == 0 && newData.getEarningsPerShare().signum() == 0) {
				continue;
			}
			boolean dataExists = false;
			for (FundamentalData oldData : fundamentalDatas) {
				if (newData.getYear() == oldData.getYear()) {
					oldData.update(newData);
					dataExists = true;
					break;
				}
			}
			if (! dataExists) {
				fundamentalDatas.add(newData);
			}
		}
		security.setFundamentalDatas(fundamentalDatas);
	}

}
