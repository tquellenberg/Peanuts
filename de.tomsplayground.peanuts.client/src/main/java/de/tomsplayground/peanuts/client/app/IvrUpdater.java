package de.tomsplayground.peanuts.client.app;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tomsplayground.peanuts.app.ib.IVR;
import de.tomsplayground.peanuts.app.ib.IbConnection;
import de.tomsplayground.peanuts.client.editors.security.properties.IvrPropertyPage;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Security;

public class IvrUpdater {

	private final static Logger log = LoggerFactory.getLogger(IvrUpdater.class);

	private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

	public void init() {
		AccountManager accountManager = Activator.getDefault().getAccountManager();
		accountManager.getSecurities().stream()
			.filter(s -> ! s.isDeleted())
			.forEach(s -> s.addPropertyChangeListener(IvrPropertyPage.IVR_CALCULATION, e -> updateIvr((Security) e.getSource())));
//		service.scheduleWithFixedDelay(this::updateAll, 2, 60, TimeUnit.MINUTES);
	}

	public void destroy() {
		service.shutdown();
	}

	public void updateAll() {
		AccountManager accountManager = Activator.getDefault().getAccountManager();
		accountManager.getSecurities().stream()
			.filter(s -> ! s.isDeleted())
			.forEach(this::updateIvr);
	}

	public IVR calculate(Security security) {
		if (BooleanUtils.toBoolean(security.getConfigurationValue(IvrPropertyPage.IVR_CALCULATION))) {
			log.info("Update IVR: "+security.getName());
			IbConnection ibConnection = Activator.getDefault().getIbConnection();
			String morningstarSymbol = security.getMorningstarSymbol();
			String symbol = StringUtils.substringAfter(morningstarSymbol, ":");
			symbol = StringUtils.replace(symbol, ".", " ");
			if (StringUtils.isNotBlank(symbol)) {
				return ibConnection.getData(symbol);
			}
		}
		return null;
	}

	public void updateIvr(Security security) {
		IVR ivr = calculate(security);
		if (ivr != null) {
			double rank = ivr.getRank();
			if (! Double.isNaN(rank)) {
				security.putConfigurationValue(IvrPropertyPage.IVR_RANK, Double.toString(rank));
				security.putConfigurationValue(IvrPropertyPage.IVR_MIN, Double.toString(ivr.getLow()));
				security.putConfigurationValue(IvrPropertyPage.IVR_MAX, Double.toString(ivr.getHigh()));
				try {
					Thread.sleep(10 * 1000);
				} catch (InterruptedException e) {
					Thread.interrupted();
				}
			}
		} else {
			security.putConfigurationValue(IvrPropertyPage.IVR_RANK, "");
			security.putConfigurationValue(IvrPropertyPage.IVR_MIN, "");
			security.putConfigurationValue(IvrPropertyPage.IVR_MAX, "");
		}
	}
}
