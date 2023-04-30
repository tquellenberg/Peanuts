package de.tomsplayground.peanuts.app.google;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.process.PriceProvider;
import de.tomsplayground.peanuts.util.Day;

public class GooglePriceReader extends PriceProvider {

	private final static Logger log = LoggerFactory.getLogger(GooglePriceReader.class);

	private final static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d-MMM-yy", Locale.US);

	private final Reader reader;

	public GooglePriceReader(Security security, String ticker) throws IOException, CsvValidationException, URISyntaxException {
		super(security);
		URL url = new URI("http://www.google.com/finance/historical?q=" + ticker + "&output=csv").toURL();
		reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8);
		read();
	}

	public GooglePriceReader(Security security, Reader reader) throws IOException, CsvValidationException {
		super(security);
		this.reader = reader;
		read();
	}

	private void read() throws IOException, CsvValidationException {
		CSVReader csvReader = new CSVReaderBuilder(reader)
			.withCSVParser(new CSVParserBuilder()
				.withSeparator(',')
				.withQuoteChar('"').build()).build();

		// Skip header
		csvReader.readNext();
		String[] values;
		while ((values = csvReader.readNext()) != null) {
			try {
				Day d = readDay(values[0]);
				BigDecimal open = values[1].length()==0?null:new BigDecimal(values[1]);
				BigDecimal close = values[4].length()==0?null:new BigDecimal(values[4]);
				if (close == null) {
					if (open != null) {
						close = open;
					} else {
						close = BigDecimal.ZERO;
					}
				}
				setPrice(new Price(d, close));
			} catch (NumberFormatException e) {
				log.error(e.getMessage() + " Value: " + Arrays.toString(values));
			} catch (IllegalArgumentException e) {
				log.error(e.getMessage() + " Value: " + Arrays.toString(values));
			}
		}
		csvReader.close();
	}

	protected Day readDay(String dayStr) {
		return Day.from(LocalDate.parse(dayStr, dateFormatter));
	}

}
