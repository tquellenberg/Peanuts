package de.tomsplayground.peanuts.client.actions;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import de.tomsplayground.peanuts.app.marketscreener.MarketScreener;
import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.INamedElement;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.fundamental.FundamentalData;
import de.tomsplayground.peanuts.domain.fundamental.FundamentalDatas;
import de.tomsplayground.peanuts.util.Day;

public class Update4TraderFundamentalData extends AbstractHandler {

	private final static int MAX_SECURITY_UPDATED_PER_RUN = 15;

	private final static int MAX_AGE_BEFORE_UPDATE = 30;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job refreshJob = new Job("Refresh fundamental data") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					List<Security> securities = securitiesToUpdate();
					MarketScreener fourTraders = new MarketScreener();
					monitor.beginTask("Refresh fundamental data", securities.size());
					for (Security security : securities) {
						
						monitor.subTask("Refreshing " + security.getName());
						updateFundamentaData(security, fourTraders.scrapFinancials(security));
						monitor.worked(1);

						try {
							Thread.sleep(1000*10);
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
		SortedMap<LocalDateTime, Security> securitiesToUpdate = new TreeMap<>();
		for (Security security : Activator.getDefault().getAccountManager().getSecurities()) {
			if (isUpdatePossible(security)) {
				LocalDateTime lastUpdateDate = getLastUpdateDate(security);
				if (lastUpdateDate.compareTo(LocalDateTime.now().minusDays(MAX_AGE_BEFORE_UPDATE)) < 0) {
					securitiesToUpdate.put(lastUpdateDate, security);
				}
			}
		}
		
		List<Security> securities = new ArrayList<>(securitiesToUpdate.values());
		while (securities.size() > MAX_SECURITY_UPDATED_PER_RUN) {
			securities.remove(securities.size()-1);
		}
		
		securities.sort(INamedElement.NAMED_ELEMENT_ORDER);
		
		return securities;
	}
	
	private LocalDateTime getLastUpdateDate(Security security) {
		FundamentalDatas fundamentalDatas = security.getFundamentalDatas();
		Optional<LocalDateTime> maxDate = fundamentalDatas.getMaxModificationDate();
		return maxDate.orElse(LocalDateTime.MIN);
	}
	
	private boolean isUpdatePossible(Security security) {
		if (security.isDeleted()) {
			return false;
		}
		String financialsUrl = security.getConfigurationValue(MarketScreener.CONFIG_KEY_URL);
		if (StringUtils.isBlank(financialsUrl)) {
			return false;
		}
		FundamentalDatas fundamentalDatas = security.getFundamentalDatas();
		if (fundamentalDatas.isEmpty()) {
			return false;
		}
		return true;
	}

	private void updateFundamentaData(Security security, List<FundamentalData> newDatas) {
		FundamentalDatas fundamentalDatas = security.getFundamentalDatas();
		Currency dataCurrency = fundamentalDatas.getCurrency();
		List<FundamentalData> existingDatas = new ArrayList<>(fundamentalDatas.getDatas());
		for (FundamentalData newData : newDatas) {
			newData.setCurrency(dataCurrency);
			if (newData.getDividende().signum() == 0 && newData.getEarningsPerShare().signum() == 0) {
				continue;
			}
			boolean dataExists = false;
			for (FundamentalData existingData : existingDatas) {
				if (newData.getYear() == existingData.getYear()) {
					// Not older than 6 months
					if (existingData.getFiscalEndDay().delta(Day.today()) <= 180) {
						existingData.update(newData);
					}
					dataExists = true;
					break;
				}
			}
			if (! dataExists) {
				existingDatas.add(newData);
			}
		}
		security.setFundamentalDatas(existingDatas);
	}

}
