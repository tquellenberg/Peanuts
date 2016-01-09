package de.tomsplayground.peanuts.domain.statistics;

import java.io.IOException;
import java.io.Writer;
import java.util.stream.Stream;

import de.tomsplayground.peanuts.domain.statistics.AccountValueData.Entry;

public class AccountValueCsvWriter {

	public static void write(Stream<AccountValueData.Entry> entries, Writer writer) {
		try {
			entries.forEach(e -> write(writer, e));
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void write(Writer writer, Entry e) {
		try {
			writer.append('"').append(e.getDay().toString()).append('"')
			.append(',')
			.append(e.getInvestment().toString())
			.append(',')
			.append(e.getValue().toString())
			.append('\n');
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}
	}
}
