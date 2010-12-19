package de.tomsplayground.peanuts.domain.process;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.text.ParseException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import au.com.bytecode.opencsv.CSVWriter;
import de.tomsplayground.peanuts.app.csv.YahooCsvReader;
import de.tomsplayground.peanuts.domain.base.Security;

public class PriceProviderFactory implements IPriceProviderFactory {

	final static Logger log = Logger.getLogger(PriceProviderFactory.class);
	
	private static String localPriceStorePath = ".";
	private static PriceProviderFactory priceProviderFactory;
	
	private Map<Security, IPriceProvider> priceProviderMap = new HashMap<Security, IPriceProvider>();
	
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
			IPriceProvider remotePriceProvider = readHistoricalPricesFromYahoo(security);
			if (remotePriceProvider != null) {
				mergePrices(localPriceProvider, remotePriceProvider);
			}
			IPriceProvider remotePriceProvider2 = readLastPricesFromYahoo(security);
			if (remotePriceProvider2 != null) {
				mergePrices(localPriceProvider, remotePriceProvider2);
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
		YahooCsvReader reader = new YahooCsvReader(new StringReader(csv));
		try {
			reader.read();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ParseException e) {
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
	
	protected IPriceProvider readHistoricalPricesFromYahoo(Security security) {
		Calendar today = Calendar.getInstance();
		InputStream stream = null;
		try {
			URL url = new URL("http://ichart.finance.yahoo.com/table.csv?g=d&a=0&b=3&c=2000" +
				"&d=" + today.get(Calendar.MONTH) + "&e=" + today.get(Calendar.DAY_OF_MONTH) +
				"&f=" + today.get(Calendar.YEAR) + "&s=" + security.getTicker());
			stream = url.openStream();
			return buildPriceProvider(IOUtils.toString(stream));
		} catch (IOException e) {
			log.error("", e);
			return null;
		} finally {
			IOUtils.closeQuietly(stream);
		}
	}

	protected IPriceProvider readLastPricesFromYahoo(Security security) {
		InputStream stream = null;
		try {
			URL url = new URL("http://de.old.finance.yahoo.com/d/quotes.csv?f=sl1d1t1c1ohgv" +
				"&s=" + security.getTicker());
			stream = url.openStream();
			String str = IOUtils.toString(stream);
			YahooCsvReader yahooCsvReader = new YahooCsvReader(new StringReader(str), YahooCsvReader.Type.CURRENT);
			yahooCsvReader.read();
			return yahooCsvReader;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(stream);
		}
	}

	public static void setLocalPriceStorePath(String localPriceStorePath) {
		PriceProviderFactory.localPriceStorePath = localPriceStorePath;
	}

}
