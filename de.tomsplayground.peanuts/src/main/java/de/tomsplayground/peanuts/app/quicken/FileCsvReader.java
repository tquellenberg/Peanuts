package de.tomsplayground.peanuts.app.quicken;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.opencsv.CSVReader;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.process.PriceProvider;
import de.tomsplayground.util.Day;

public class FileCsvReader extends PriceProvider {

	private final DateFormat dateFormat = new SimpleDateFormat("dd.MM.yy");
	private final CSVReader csvReader;

	public FileCsvReader(Security security, Reader reader) {
		super(security);
		csvReader = new CSVReader(reader, '\t');
	}

	public void read() throws IOException, ParseException {
		String values[];
		// Skip header
		csvReader.readNext();
		while ((values = csvReader.readNext()) != null) {
			Date d = dateFormat.parse(values[0]);
			BigDecimal close = new BigDecimal(values[1].replace(',', '.'));
			setPrice(new Price(Day.fromDate(d), close));
		}
	}

	@Override
	public String getName() {
		return "File";
	}

}
