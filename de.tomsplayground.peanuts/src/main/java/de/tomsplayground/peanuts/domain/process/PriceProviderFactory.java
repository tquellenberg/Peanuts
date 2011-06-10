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
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import au.com.bytecode.opencsv.CSVWriter;
import de.tomsplayground.peanuts.app.csv.GoogleCsvPriceProvider;
import de.tomsplayground.peanuts.app.csv.YahooCsvReader;
import de.tomsplayground.peanuts.app.csv.YahooCsvReader.Type;
import de.tomsplayground.peanuts.domain.base.Security;

public class PriceProviderFactory implements IPriceProviderFactory {

	private static final String GOOGLE_PREFIX = "Google:";

	final static Logger log = Logger.getLogger(PriceProviderFactory.class);
	
	private static String localPriceStorePath = ".";
	private static PriceProviderFactory priceProviderFactory;
	
	private final Map<Security, IPriceProvider> priceProviderMap = new HashMap<Security, IPriceProvider>();
	
	private PriceProviderFactory() {
		// private constructor
	}

	public static synchronized PriceProviderFactory getInstance() {
		if (priceProviderFactory == null)
			priceProviderFactory = new PriceProviderFactory();
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

	public void refresh(Security security) {
		if (StringUtils.isNotBlank(security.getTicker())) {
			IPriceProvider localPriceProvider = getPriceProvider(security);
			if (security.getTicker().startsWith(GOOGLE_PREFIX)) {
				String ticker = StringUtils.removeStart(security.getTicker(), GOOGLE_PREFIX);
				IPriceProvider remotePriceProvider = readHistoricalPricesFromGoogle(ticker);
				if (remotePriceProvider != null) {
					mergePrices(localPriceProvider, remotePriceProvider);
				}
			} else {
				IPriceProvider remotePriceProvider = readHistoricalPricesFromYahoo(security);
				if (remotePriceProvider != null) {
					mergePrices(localPriceProvider, remotePriceProvider);
				}
				IPriceProvider remotePriceProvider2 = readLastPricesFromYahoo(security);
				if (remotePriceProvider2 != null) {
					mergePrices(localPriceProvider, remotePriceProvider2);
				}
			}
			saveToLocal(security, localPriceProvider);
		}		
	}

	private void mergePrices(IPriceProvider localPriceProvider, IPriceProvider remotePriceProvider) {
		List<Price> prices = remotePriceProvider.getPrices();
		localPriceProvider.setPrices(prices, true);
	}

	protected String localFilename(Security security) {
		return localPriceStorePath + File.separator + security.getISIN() + ".csv";
	}
	
	protected IPriceProvider buildPriceProvider(String csv) {
		IPriceProvider reader;
		try {
			reader = new YahooCsvReader(new StringReader(csv));
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
				List<Price> prices = priceProvider.getPrices();
				
				CSVWriter csvWriter = new CSVWriter(writer, ',', '"');
				String line[] = {"Date","Open","High","Low","Close","Volume","Adj Close"};
				csvWriter.writeNext(line);
				ListIterator<Price> iterator = prices.listIterator(prices.size());
				while (iterator.hasPrevious()) {
					Price p = iterator.previous();
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
					return buildPriceProvider(IOUtils.toString(reader));
				} catch (IOException e) {
					return null;
				} finally {
					IOUtils.closeQuietly(reader);
				}
			}
	 		return new EmptyPriceProvider();
		}
	}
	
	protected IPriceProvider readHistoricalPricesFromGoogle(String ticker) {
		try {
			return new GoogleCsvPriceProvider(ticker);
		} catch (IOException e) {
			log.error("", e);
			return null;
		}
	}

	protected IPriceProvider readHistoricalPricesFromYahoo(Security security) {
		try {
			return YahooCsvReader.forTicker(security.getTicker(), Type.HISTORICAL);
		} catch (IOException e) {
			log.error("", e);
			return null;
		}
	}

	protected IPriceProvider readLastPricesFromYahoo(Security security) {
		try {
			return YahooCsvReader.forTicker(security.getTicker(), Type.CURRENT);
		} catch (IOException e) {
			log.error("", e);
			return null;
		}
	}

	public static void setLocalPriceStorePath(String localPriceStorePath) {
		PriceProviderFactory.localPriceStorePath = localPriceStorePath;
	}

}
