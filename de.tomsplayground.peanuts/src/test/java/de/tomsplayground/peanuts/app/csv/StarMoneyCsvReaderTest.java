package de.tomsplayground.peanuts.app.csv;

import static org.junit.Assert.assertEquals;

import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.List;

import org.junit.Test;

import de.tomsplayground.peanuts.domain.process.BankTransaction;
import de.tomsplayground.util.Day;


public class StarMoneyCsvReaderTest {

	@Test
	public void testRead() throws Exception {
		Reader in = new InputStreamReader(StarMoneyCsvReaderTest.class.getResourceAsStream("/starmoney_transactions.csv"), Charset.forName("iso-8859-1"));
		StarMoneyCsvReader csvReader = new StarMoneyCsvReader(in);
		csvReader.read();

		List<BankTransaction> transactions = csvReader.getTransactions();
		assertEquals(2, transactions.size());
		
		BankTransaction t1 = transactions.get(0);
		assertEquals(0, (new BigDecimal("-1130.29")).compareTo(t1.getAmount()));
		assertEquals(new Day(2001, 9, 8), t1.getDay());
		assertEquals("VISAKARTE  4901580129075951", t1.getLabel());
		assertEquals("ABRECHNUNG VOM 27.09.01\nBELASTUNG URSPRÜNGLICH\nEUR 577,91-", t1.getMemo());
	}
}
