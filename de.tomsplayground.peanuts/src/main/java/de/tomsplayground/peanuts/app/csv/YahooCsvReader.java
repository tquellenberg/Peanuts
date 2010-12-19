package de.tomsplayground.peanuts.app.csv;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

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
	
	private final DateFormat dateFormat2 = new SimpleDateFormat("dd/MM/yyyy");
	private final CSVReader csvReader;
	private final Type type;

	public YahooCsvReader(Reader reader) {
		this(reader, Type.HISTORICAL);
	}
	
	public YahooCsvReader(Reader reader, Type type) {
		if (type == Type.HISTORICAL)
			csvReader = new CSVReader(reader, ',', '"');
		else
			csvReader = new CSVReader(reader, ';', '"');
		this.type = type;
	}

	public void read() throws IOException, ParseException {
		String values[];
		if (type == Type.HISTORICAL) {
			// Skip header
			csvReader.readNext();
			while ((values = csvReader.readNext()) != null) {
				try {
					Day d = Day.fromString(values[0]);
					BigDecimal open = values[1].length()==0?null:new BigDecimal(values[1]);
					BigDecimal high = values[2].length()==0?null:new BigDecimal(values[2]);
					BigDecimal low = values[3].length()==0?null:new BigDecimal(values[3]);
					BigDecimal close = values[4].length()==0?null:new BigDecimal(values[4]);
					setPrice(new Price(d, open, close, high, low));
				} catch (NumberFormatException e) {
					System.err.println("Value: " + Arrays.toString(values));
					throw e;
				} catch (IllegalArgumentException e) {
					System.err.println("Value: " + Arrays.toString(values));
					throw e;
				}
			}
		} else {
			values = csvReader.readNext();
			if (values != null && values.length >= 7) {
				int startPos;
				if (values[2].equals("N/A")) {
					startPos = 4;
				} else {
					startPos = 3;
				}
				Date d = dateFormat2.parse(values[startPos]);
				BigDecimal close = readDecimal(values[1]);
				BigDecimal open = readDecimal(values[startPos+2]);
				BigDecimal high = readDecimal(values[startPos+3]);
				BigDecimal low = readDecimal(values[startPos+4]);
				setPrice(new Price(Day.fromDate(d), open, close, high, low));
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
