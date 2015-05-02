package de.tomsplayground.peanuts.app.google;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.process.PriceProvider;
import de.tomsplayground.util.Day;

public class GooglePriceReader extends PriceProvider {

	private final static Logger log = LoggerFactory.getLogger(GooglePriceReader.class);

	private URL url;
	private final Reader reader;

	public GooglePriceReader(Security security, String ticker) throws IOException {
		super(security);
		url = new URL("http://www.google.com/finance/historical?q=" + ticker + "&output=csv");
		reader = new InputStreamReader(url.openStream(), "UTF-8");
		read();
	}

	public GooglePriceReader(Security security, Reader reader) throws IOException {
		super(security);
		this.reader = reader;
		read();
	}

	private void read() throws IOException {
		CSVReader csvReader = new CSVReader(reader, ',', '"');
		// Skip header
		csvReader.readNext();
		String[] values;
		while ((values = csvReader.readNext()) != null) {
			try {
				Day d = readDay(values[0]);
				BigDecimal open = values[1].length()==0?null:new BigDecimal(values[1]);
				BigDecimal high = values[2].length()==0?null:new BigDecimal(values[2]);
				BigDecimal low = values[3].length()==0?null:new BigDecimal(values[3]);
				BigDecimal close = values[4].length()==0?null:new BigDecimal(values[4]);
				setPrice(new Price(d, open, close, high, low));
			} catch (ParseException e) {
				log.error(e.getMessage() + " Value: " + Arrays.toString(values));
			} catch (NumberFormatException e) {
				log.error(e.getMessage() + " Value: " + Arrays.toString(values));
			} catch (IllegalArgumentException e) {
				log.error(e.getMessage() + " Value: " + Arrays.toString(values));
			}
		}
		csvReader.close();
	}

	protected Day readDay(String dayStr) throws ParseException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yy", Locale.US);
		Date date = dateFormat.parse(dayStr);
		return Day.fromDate(date);
	}

	@Override
	public String getName() {
		return "Google";
	}
}
