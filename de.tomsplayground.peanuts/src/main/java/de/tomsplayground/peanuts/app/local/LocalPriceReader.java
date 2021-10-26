package de.tomsplayground.peanuts.app.local;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.process.PriceProvider;
import de.tomsplayground.peanuts.util.Day;

public class LocalPriceReader extends PriceProvider {

	private final static Logger log = LoggerFactory.getLogger(LocalPriceReader.class);

	private final CSVReader csvReader;

	public LocalPriceReader(Security security, Reader reader) throws IOException, CsvValidationException {
		super(security);
		csvReader = new CSVReaderBuilder(reader)
			.withCSVParser(new CSVParserBuilder()
				.withSeparator(',')
				.withQuoteChar('"').build()).build();
		read();
		csvReader.close();
	}

	private void read() throws IOException, CsvValidationException {
		String values[];
		List<IPrice> prices = new ArrayList<>();
		// Skip header
		csvReader.readNext();
		while ((values = csvReader.readNext()) != null) {
			if (StringUtils.isNotBlank(values[0])) {
				try {
					Day d = Day.fromString(values[0]);
					if (d.year < 2100 && ! d.equals(Day._1_1_1970)) {
						BigDecimal close = getValue(values, 4);
						Price price = new Price(d, close);
						prices.add(price);
					}
				} catch (NumberFormatException e) {
					log.error("Value: " + Arrays.toString(values));
					throw e;
				} catch (IllegalArgumentException e) {
					log.error("Value: " + Arrays.toString(values));
					throw e;
				}
			}
		}
		setPrices(prices, true);
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

}
