package de.tomsplayground.peanuts.app.yahoo;

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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.process.PriceProvider;
import de.tomsplayground.util.Day;

public class YahooPriceReader extends PriceProvider {

	private final static Logger log = LoggerFactory.getLogger(YahooPriceReader.class);

	public enum Type {
		CURRENT,
		HISTORICAL
	}

	private final DateFormat dateFormat2 = new SimpleDateFormat("MM/dd/yyyy");
	private final CSVReader csvReader;
	private final Type type;

	public static YahooPriceReader forTicker(Security security, String ticker, Type type) throws IOException {
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
		return new YahooPriceReader(security, new StringReader(str), type);
	}

	public YahooPriceReader(Security security, Reader reader) throws IOException {
		this(security, reader, Type.HISTORICAL);
	}

	public YahooPriceReader(Security security, Reader reader, Type type) throws IOException {
		super(security);
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
						BigDecimal open = getValue(values, 1);
						BigDecimal high = getValue(values, 2);
						BigDecimal low = getValue(values, 3);
						BigDecimal close = getValue(values, 4);
						Price price = new Price(d, open, close, high, low);
						if (price.getValue().compareTo(BigDecimal.ZERO) > 0) {
							setPrice(price);
						}
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

	private BigDecimal getValue(String[] values, int col) {
		if (col >= values.length || StringUtils.isBlank(values[col])) {
			return null;
		}
		try {
			return new BigDecimal(values[col]);
		} catch (NumberFormatException e) {
			log.error("Invalid input: " + Arrays.toString(values));
			return null;
		}
	}

	private BigDecimal readDecimal(String value) {
		value = value.replace(',', '.');
		try  {
			return new BigDecimal(value);
		} catch (NumberFormatException e) {
			log.error("readDecimal: '" + value+"' "+e.getMessage());
			return null;
		}
	}

	@Override
	public String getName() {
		return "Yahoo";
	}

}
