package de.tomsplayground.peanuts.app.csv;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import au.com.bytecode.opencsv.CSVReader;
import de.tomsplayground.peanuts.domain.process.BankTransaction;
import de.tomsplayground.util.Day;

public class StarMoneyCsvReader {

	private final CSVReader csvReader;
	private List<BankTransaction> transactions;

	public StarMoneyCsvReader(Reader reader) {
		csvReader = new CSVReader(reader, ';');
	}

	public void read() throws IOException, ParseException {
		transactions = new ArrayList<BankTransaction>();
		// Skip header
		csvReader.readNext();
		String[] values;
		SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
		while ((values = csvReader.readNext()) != null) {
			Day date = Day.fromDate(format.parse(values[3]));
			BigDecimal amount = new BigDecimal(values[0].replace(',', '.'));
			String label = values[26];
			String memo = "";
			for (int i = 27; i < 39; i++) {
				if (StringUtils.isNotBlank(values[i])) {
					if (memo.length() > 0) {
						memo += "\n";
					}
					memo += values[i];
				}
			}
			BankTransaction transaction = new BankTransaction(date, amount, label);
			transaction.setMemo(memo);
			transactions.add(transaction);
		}
	}

	public List<BankTransaction> getTransactions() {
		return transactions;
	}

}
