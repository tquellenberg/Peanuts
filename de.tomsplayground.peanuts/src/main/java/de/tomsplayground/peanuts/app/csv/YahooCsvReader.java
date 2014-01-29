package de.tomsplayground.peanuts.app.csv;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import au.com.bytecode.opencsv.CSVReader;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.process.PriceProvider;
import de.tomsplayground.util.Day;

public class YahooCsvReader extends PriceProvider {

	final static Logger log = Logger.getLogger(YahooCsvReader.class);
	
	public enum Type {
		CURRENT,
		HISTORICAL
	}
	
	private final DateFormat dateFormat2 = new SimpleDateFormat("MM/dd/yyyy");
	private final CSVReader csvReader;
	private final Type type;

	public static YahooCsvReader forTicker(String ticker, Type type) throws IOException {
		URL url;
		if (type == Type.CURRENT) {
			url = new URL("http://download.finance.yahoo.com/d/quotes.csv?f=sl1d1t1c1ohgv&s=" +
					ticker);
		} else {
			Calendar today = Calendar.getInstance();
			url = new URL("http://ichart.finance.yahoo.com/table.csv?g=d&a=0&b=3&c=2000" +
				"&d=" + today.get(Calendar.MONTH) + "&e=" + today.get(Calendar.DAY_OF_MONTH) +
				"&f=" + today.get(Calendar.YEAR) + "&s=" + ticker);
		}
		URLConnection connection = url.openConnection();
		connection.setConnectTimeout(1000*10);
		String str = IOUtils.toString(connection.getInputStream());
		return new YahooCsvReader(new StringReader(str), type);
	}
	
	public YahooCsvReader(Reader reader) throws IOException {
		this(reader, Type.HISTORICAL);
	}
	
	public YahooCsvReader(Reader reader, Type type) throws IOException {
		if (type == Type.HISTORICAL) {
			csvReader = new CSVReader(reader, ',', '"');
		} else {
			csvReader = new CSVReader(reader, ',', '"');
		}
		this.type = type;
		read();
	}

	private void read() throws IOException {
		String values[];
		if (type == Type.HISTORICAL) {
			// Skip header
			csvReader.readNext();
			while ((values = csvReader.readNext()) != null) {
				try {
					Day d = Day.fromString(values[0]);
					if (d.year < 3000) {
						BigDecimal open = values[1].length()==0?null:new BigDecimal(values[1]);
						BigDecimal high = values[2].length()==0?null:new BigDecimal(values[2]);
						BigDecimal low = values[3].length()==0?null:new BigDecimal(values[3]);
						BigDecimal close = values[4].length()==0?null:new BigDecimal(values[4]);
						setPrice(new Price(d, open, close, high, low));
					}
				} catch (NumberFormatException e) {
					log.error("Value: " + Arrays.toString(values));
					throw e;
				} catch (IllegalArgumentException e) {
					log.error("Value: " + Arrays.toString(values));
					throw e;
				}
			}
		} else {
			values = csvReader.readNext();
			if (values != null && values.length >= 7 && !values[2].equals("N/A")) {
				int startPos = 5;
				try {
					Day d = Day.fromDate(dateFormat2.parse(values[2]));
					if (d.year < 3000) {
						BigDecimal close = readDecimal(values[1]);
						BigDecimal open = readDecimal(values[startPos]);
						BigDecimal high = readDecimal(values[startPos+1]);
						BigDecimal low = readDecimal(values[startPos+2]);
						setPrice(new Price(d, open, close, high, low));
					}
				} catch (ParseException e) {
					log.error(e.getMessage()+ " Value: " + Arrays.toString(values), e);
				}
			} else {
				log.error("Invalid input: " + Arrays.toString(values));
			}
		}
		csvReader.close();
	}

	private BigDecimal readDecimal(String value) {
		value = value.replace(',', '.');
		try  {
			return new BigDecimal(value);
		} catch (NumberFormatException e) {
			System.err.println("readDecimal: " + value);
			return null;
		}
	}
	
	@Override
	public String getName() {
		return "Yahoo";
	}

}
