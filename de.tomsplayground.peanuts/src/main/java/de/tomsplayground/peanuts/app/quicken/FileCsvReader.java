package de.tomsplayground.peanuts.app.quicken;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.process.PriceProvider;
import de.tomsplayground.peanuts.util.Day;

public class FileCsvReader extends PriceProvider {

	private final static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yy");

	private final CSVReader csvReader;

	public FileCsvReader(Security security, Reader reader) {
		super(security);
		csvReader = new CSVReaderBuilder(reader)
			.withCSVParser(new CSVParserBuilder()
				.withSeparator('\t').build()).build();
	}

	public void read() throws IOException, CsvValidationException {
		String values[];
		// Skip header
		csvReader.readNext();
		while ((values = csvReader.readNext()) != null) {
			LocalDate d = LocalDate.parse(values[0], dateFormatter);
			BigDecimal close = new BigDecimal(values[1].replace(',', '.'));
			setPrice(new Price(Day.from(d), close));
		}
	}

}
