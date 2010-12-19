package de.tomsplayground.peanuts.domain.reporting.investment;

import static org.junit.Assert.assertEquals;


import java.math.BigDecimal;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.tomsplayground.peanuts.Helper;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.BankTransaction;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.IPriceProviderFactory;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.process.PriceProvider;
import de.tomsplayground.peanuts.domain.process.Transfer;
import de.tomsplayground.peanuts.domain.reporting.investment.PerformanceAnalyzer.Value;
import de.tomsplayground.util.Day;

public class PerformanceAnalyzerTest {

	private AccountManager accountManager;
	private Account account;
	private IPriceProviderFactory priceProviderFactory;

	private static class SimplePriceProvider extends PriceProvider {

		@Override
		public String getName() {
			return "test";
		}
		
		public void addPrice(Price price) {
			setPrice(price);
		}
	}

	@Before
	public void setup() {
		accountManager = new AccountManager();
		account = accountManager.getOrCreateAccount("X", Account.Type.INVESTMENT);
		priceProviderFactory = new IPriceProviderFactory() {
			@Override
			public IPriceProvider getPriceProvider(Security security) {
				SimplePriceProvider simplePriceProvider = new SimplePriceProvider();
				simplePriceProvider.addPrice(new Price(new Day(2008, 3, 13), new BigDecimal("9.00")));
				return simplePriceProvider;
			}};
	}
	
	@Test
	public void testEmptyAccount() throws Exception {
		PerformanceAnalyzer analizer = new PerformanceAnalyzer(account, priceProviderFactory);

		List<Value> values = analizer.getValues();
		Assert.assertEquals(0, values.size());
	}
	
	@Test
	public void testMarketValueWithMoney() throws Exception {
		account.addTransaction(new BankTransaction(new Day(), new BigDecimal("100.00"), "l"));
		InvestmentTransaction transaction = new InvestmentTransaction(new Day(), 
				new Security("AAPL"), BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, InvestmentTransaction.Type.BUY);
		account.addTransaction(transaction);
		PerformanceAnalyzer analizer = new PerformanceAnalyzer(account, priceProviderFactory);

		List<Value> values = analizer.getValues();
		Assert.assertEquals(1, values.size());
		Value value = values.get(0);
		Helper.assertEquals(BigDecimal.ZERO, value.getMarketValue1());
		Assert.assertEquals(new Day().getYear(), value.getYear());
		// 100 - (10 * 1) + (10 * 9)
		Helper.assertEquals(new BigDecimal("180.00"), value.getMarketValue2());
	}
	
	@Test
	public void testLastYear() {
		Day d = new Day();
		d = d.addYear(-1);
		account.addTransaction(new BankTransaction(d, new BigDecimal("100.00"), "l"));
		PerformanceAnalyzer analizer = new PerformanceAnalyzer(account, priceProviderFactory);		

		List<Value> values = analizer.getValues();
		Assert.assertEquals(2, values.size());
	
		assertEquals(new Day().getYear() - 1, values.get(0).getYear());
		assertEquals(new Day().getYear(), values.get(1).getYear());
	}

	@Test
	public void testAdditionsLeavings() throws Exception {
		Account account2 = accountManager.getOrCreateAccount("X2", Account.Type.BANK);
		// 1. Transfer (+100)
		Transfer transfer = new Transfer(account2, account, new BigDecimal("100.00"), new Day());
		account2.addTransaction(transfer.getTransferFrom());
		account.addTransaction(transfer.getTransferTo());
		// 2. Bank transaction  (-1)
		account.addTransaction(new BankTransaction(new Day(), new BigDecimal("-1.00"), ""));
		// 3. Transfer (-10)
		transfer = new Transfer(account, account2, new BigDecimal("10.00"), new Day());
		account2.addTransaction(transfer.getTransferTo());
		account.addTransaction(transfer.getTransferFrom());
		// 4. Bank transaction (+3)
		account.addTransaction(new BankTransaction(new Day(), new BigDecimal("3.00"), ""));
		PerformanceAnalyzer analizer = new PerformanceAnalyzer(account, priceProviderFactory);
		
		List<Value> values = analizer.getValues();
		Assert.assertEquals(1, values.size());
		Value value = values.get(0);
		Helper.assertEquals(new BigDecimal("100.00"), value.getAdditions());
		Helper.assertEquals(new BigDecimal("-10.00"), value.getLeavings());
	}
	
	@Test
	public void testAdditionLeavingsFromSplit() throws Exception {
		Account account2 = accountManager.getOrCreateAccount("X2", Account.Type.BANK);
		// 1 Transaction
		BankTransaction split = new BankTransaction(new Day(),BigDecimal.ZERO, "");
		Transfer transfer = new Transfer(account2, account, new BigDecimal("100.00"), new Day());
		account2.addTransaction(transfer.getTransferFrom());
		// 1.1 Split: Transfer (+100)
		split.addSplit(transfer.getTransferTo());
		// 1.2 Split: Bank transaction (-1)
		split.addSplit(new BankTransaction(new Day(), new BigDecimal("-1.00"), ""));
		account.addTransaction(split);
		// 2 Transaction
		transfer = new Transfer(account, account2, new BigDecimal("10.00"), new Day());
		account2.addTransaction(transfer.getTransferTo());
		split = new BankTransaction(new Day(),BigDecimal.ZERO, "");
		// 2.1 Split: Transfer (-10)
		split.addSplit(transfer.getTransferFrom());
		// 2.2 Split: Bank transaction (+3)
		split.addSplit(new BankTransaction(new Day(), new BigDecimal("3.00"), ""));
		account.addTransaction(split);
		PerformanceAnalyzer analizer = new PerformanceAnalyzer(account, priceProviderFactory);
		
		List<Value> values = analizer.getValues();
		Assert.assertEquals(1, values.size());
		Value value = values.get(0);
		Helper.assertEquals(new BigDecimal("100.00"), value.getAdditions());
		Helper.assertEquals(new BigDecimal("-10.00"), value.getLeavings());
	}

	@Test
	public void testInvestedAvg() throws Exception {
		Account account2 = accountManager.getOrCreateAccount("X2", Account.Type.BANK);

		// 1. Addition on 1.7.2010 (+100)
		Transfer transfer = new Transfer(account2, account, new BigDecimal("100.00"), new Day(2010, 6, 1));
		account2.addTransaction(transfer.getTransferFrom());
		account.addTransaction(transfer.getTransferTo());		
		PerformanceAnalyzer analizer = new PerformanceAnalyzer(account, priceProviderFactory);

		Value value = analizer.getValues().get(0);
		Helper.assertEquals(new BigDecimal("50.00"), value.getInvestedAvg());		
	}

	@Test
	public void testInvestedAvg2() throws Exception {
		Account account2 = accountManager.getOrCreateAccount("X2", Account.Type.BANK);

		// 1. Addition on 1.4.2010 (+100)
		Transfer transfer = new Transfer(account2, account, new BigDecimal("100.00"), new Day(2010, 3, 1));
		account2.addTransaction(transfer.getTransferFrom());
		account.addTransaction(transfer.getTransferTo());

		// 2. Leaving on 1.7.2010 (1100)
		transfer = new Transfer(account2, account, new BigDecimal("-100.00"), new Day(2010, 6, 1));
		account2.addTransaction(transfer.getTransferFrom());
		account.addTransaction(transfer.getTransferTo());		
		PerformanceAnalyzer analizer = new PerformanceAnalyzer(account, priceProviderFactory);

		Value value = analizer.getValues().get(0);
		Helper.assertEquals(new BigDecimal("25.00"), value.getInvestedAvg());		
	}	

}
