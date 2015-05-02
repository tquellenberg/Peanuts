package de.tomsplayground.peanuts.domain.process;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVWriter;
import de.tomsplayground.peanuts.app.google.GooglePriceReader;
import de.tomsplayground.peanuts.app.yahoo.YahooPriceReader;
import de.tomsplayground.peanuts.app.yahoo.YahooPriceReader.Type;
import de.tomsplayground.peanuts.domain.base.Security;

public class PriceProviderFactory implements IPriceProviderFactory {

	private static final String GOOGLE_PREFIX = "Google:";

	private final static Logger log = LoggerFactory.getLogger(PriceProviderFactory.class);

	private static String localPriceStorePath = ".";
	private static PriceProviderFactory priceProviderFactory;

	private final Map<Security, IPriceProvider> priceProviderMap = new HashMap<Security, IPriceProvider>();

	private PriceProviderFactory() {
		// private constructor
	}

	public static synchronized PriceProviderFactory getInstance() {
		if (priceProviderFactory == null) {
			priceProviderFactory = new PriceProviderFactory();
		}
		return priceProviderFactory;
	}

	@Override
	public IPriceProvider getPriceProvider(Security security) {
		IPriceProvider localPriceProvider;
		synchronized (priceProviderMap) {
			localPriceProvider = priceProviderMap.get(security);
			if (localPriceProvider == null) {
				localPriceProvider = readFromLocal(security);
				priceProviderMap.put(security, localPriceProvider);
			}
		}
		return localPriceProvider;
	}

	@Override
	public IPriceProvider getAdjustedPriceProvider(Security security, List<StockSplit> stockSplits) {
		IPriceProvider rawPriceProvider = getPriceProvider(security);
		if (stockSplits.isEmpty()) {
			return rawPriceProvider;
		}
		return new SplitAdjustedPriceProvider(rawPriceProvider, stockSplits);
	}

	public void refresh(Security security, boolean overideExistingData) {
		if (StringUtils.isNotBlank(security.getTicker())) {
			IPriceProvider localPriceProvider = getPriceProvider(security);
			if (security.getTicker().startsWith(GOOGLE_PREFIX)) {
				IPriceProvider remotePriceProvider = readHistoricalPricesFromGoogle(security);
				if (remotePriceProvider != null) {
					mergePrices(localPriceProvider, remotePriceProvider, overideExistingData);
				}
			} else {
				IPriceProvider remotePriceProvider = readHistoricalPricesFromYahoo(security);
				if (remotePriceProvider != null) {
					mergePrices(localPriceProvider, remotePriceProvider, overideExistingData);
				}
				IPriceProvider remotePriceProvider2 = readLastPricesFromYahoo(security);
				if (remotePriceProvider2 != null) {
					mergePrices(localPriceProvider, remotePriceProvider2, true);
				}
			}
			saveToLocal(security, localPriceProvider);
		}
	}

	private void mergePrices(IPriceProvider localPriceProvider, IPriceProvider remotePriceProvider, boolean overideExistingData) {
		List<IPrice> prices = remotePriceProvider.getPrices();
		localPriceProvider.setPrices(prices, overideExistingData);
	}

	protected String localFilename(Security security) {
		return localPriceStorePath + File.separator + security.getISIN() + ".csv";
	}

	protected IPriceProvider buildPriceProvider(Security security, String csv) {
		IPriceProvider reader;
		try {
			reader = new YahooPriceReader(security, new StringReader(csv));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return reader;
	}

	public void saveToLocal(Security security, IPriceProvider priceProvider) {
		synchronized (security) {
			File file = new File(localFilename(security));
			FileWriter writer = null;
			try {
				writer = new FileWriter(file);
				List<IPrice> prices = priceProvider.getPrices();

				CSVWriter csvWriter = new CSVWriter(writer, ',', '"');
				String line[] = {"Date","Open","High","Low","Close","Volume","Adj Close"};
				csvWriter.writeNext(line);
				ListIterator<IPrice> iterator = prices.listIterator(prices.size());
				while (iterator.hasPrevious()) {
					IPrice p = iterator.previous();
					line[0] = p.getDay().toString();
					line[1] = p.getOpen()==null?"":p.getOpen().toString();
					line[2] = p.getHigh()==null?"":p.getHigh().toString();
					line[3] = p.getLow()==null?"":p.getLow().toString();
					line[4] = p.getClose()==null?"":p.getClose().toString();
					line[5] = "0";
					line[6] = line[4];
					csvWriter.writeNext(line);
				}
				csvWriter.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			} finally {
				IOUtils.closeQuietly(writer);
			}
		}
	}

	protected IPriceProvider readFromLocal(Security security) {
		synchronized (security) {
			File file = new File(localFilename(security));
			FileReader reader = null;
			if (file.canRead()) {
				try {
					reader = new FileReader(file);
					return buildPriceProvider(security, IOUtils.toString(reader));
				} catch (IOException e) {
					return null;
				} finally {
					IOUtils.closeQuietly(reader);
				}
			}
			return new EmptyPriceProvider(security);
		}
	}

	protected IPriceProvider readHistoricalPricesFromGoogle(Security security) {
		String ticker = StringUtils.removeStart(security.getTicker(), GOOGLE_PREFIX);
		try {
			return new GooglePriceReader(security, ticker);
		} catch (IOException e) {
			log.error("readHistoricalPricesFromGoogle " + security.getName() + " " + e.getMessage());
			return null;
		}
	}

	protected IPriceProvider readHistoricalPricesFromYahoo(Security security) {
		try {
			return YahooPriceReader.forTicker(security, security.getTicker(), Type.HISTORICAL);
		} catch (IOException e) {
			log.error("readHistoricalPricesFromYahoo " + security.getName() + " " + e.getMessage());
			return null;
		}
	}

	protected IPriceProvider readLastPricesFromYahoo(Security security) {
		try {
			return YahooPriceReader.forTicker(security, security.getTicker(), Type.CURRENT);
		} catch (IOException e) {
			log.error("readLastPricesFromYahoo " + security.getName() + " " + e.getMessage());
			return null;
		}
	}

	public static void setLocalPriceStorePath(String localPriceStorePath) {
		PriceProviderFactory.localPriceStorePath = localPriceStorePath;
	}

}
