package de.tomsplayground.peanuts.domain.process;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import de.tomsplayground.peanuts.app.google.GooglePriceReader;
import de.tomsplayground.peanuts.app.local.LocalPriceReader;
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
	public IPriceProvider getSplitAdjustedPriceProvider(Security security, List<StockSplit> stockSplits) {
		IPriceProvider rawPriceProvider = getPriceProvider(security);
		if (stockSplits.isEmpty()) {
			return rawPriceProvider;
		}
		return new SplitAdjustedPriceProvider(rawPriceProvider, stockSplits);
	}

	public void refresh(Security security, boolean overideExistingData) {
		if (StringUtils.isNotBlank(security.getTicker())) {
			boolean changed = false;
			IPriceProvider localPriceProvider = getPriceProvider(security);
			if (security.getTicker().startsWith(GOOGLE_PREFIX)) {
				IPriceProvider remotePriceProvider = readHistoricalPricesFromGoogle(security);
				if (remotePriceProvider != null) {
					changed = mergePrices(localPriceProvider, remotePriceProvider, overideExistingData);
				}
			} else {
				boolean full = localPriceProvider.getPrices().size() < 100 || overideExistingData;
				IPriceProvider remotePriceProvider = readHistoricalPricesFromYahoo(security, full);
				if (remotePriceProvider != null) {
					changed = mergePrices(localPriceProvider, remotePriceProvider, overideExistingData);
				}
			}
			if (changed) {
				saveToLocal(security, localPriceProvider);
			}
		}
	}

	private boolean mergePrices(IPriceProvider localPriceProvider, IPriceProvider remotePriceProvider, boolean overideExistingData) {
		return localPriceProvider.setPrices(remotePriceProvider.getPrices(), overideExistingData);
	}

	protected String localFilename(Security security) {
		return localPriceStorePath + File.separator + security.getISIN() + ".csv";
	}

	public void saveToLocal(Security security, IPriceProvider priceProvider) {
		synchronized (security) {
			log.info("Saving prices for {}", security.getName());
			File file = new File(localFilename(security));
			try (Writer writer = new BufferedWriter(new FileWriter(file))) {
				List<IPrice> prices = priceProvider.getPrices();

				ICSVWriter csvWriter = new CSVWriterBuilder(writer)
					.withSeparator(',')
					.withQuoteChar('"').build();

				String line[] = {"Date","Open","High","Low","Close","Volume","Adj Close"};
				csvWriter.writeNext(line);
				ListIterator<IPrice> iterator = prices.listIterator(prices.size());
				while (iterator.hasPrevious()) {
					IPrice p = iterator.previous();
					line[0] = p.getDay().toString();
					line[1] = "";
					line[2] = "";
					line[3] = "";
					line[4] = p.getValue()==null?"":p.getValue().toString();
					line[5] = "0";
					line[6] = "";
					csvWriter.writeNext(line);
				}
				csvWriter.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private IPriceProvider readFromLocal(Security security) {
		synchronized (security) {
			File file = new File(localFilename(security));
			if (file.canRead()) {
				try (FileReader reader = new FileReader(file)) {
					return buildPriceProvider(security, reader);
				} catch (IOException e) {
					return null;
				}
			}
			return new PriceProvider(security);
		}
	}

	private IPriceProvider buildPriceProvider(Security security, Reader csv) {
		IPriceProvider reader;
		try {
			reader = new LocalPriceReader(security, csv);
		} catch (IOException | CsvValidationException e) {
			log.error("buildPriceProvider " + security.getName() + " " + e.getMessage());
			return null;
		}
		return reader;
	}

	private IPriceProvider readHistoricalPricesFromGoogle(Security security) {
		String ticker = StringUtils.removeStart(security.getTicker(), GOOGLE_PREFIX);
		try {
			return new GooglePriceReader(security, ticker);
		} catch (IOException | CsvValidationException | URISyntaxException e) {
			log.error("readHistoricalPricesFromGoogle " + security.getName() + " " + e.getMessage());
			return null;
		}
	}

	protected IPriceProvider readHistoricalPricesFromYahoo(Security security, boolean full) {
		try {
			return YahooPriceReader.forTicker(security, full?Type.HISTORICAL:Type.LAST_DAYS);
		} catch (IOException e) {
			log.error("readHistoricalPricesFromYahoo " + security.getName() + " " + e.getMessage());
			return null;
		}
	}

	protected IPriceProvider readLastPricesFromYahoo(Security security) {
		try {
			return YahooPriceReader.forTicker(security, Type.CURRENT);
		} catch (IOException e) {
			log.error("readLastPricesFromYahoo " + security.getName() + " " + e.getMessage());
			return null;
		}
	}

	public static void setLocalPriceStorePath(String localPriceStorePath) {
		PriceProviderFactory.localPriceStorePath = localPriceStorePath;
	}

}
