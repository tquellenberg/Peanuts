package de.tomsplayground.peanuts.domain.base;

import static org.junit.Assert.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Currency;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.tomsplayground.peanuts.Helper;
import de.tomsplayground.peanuts.domain.process.BankTransaction;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.IPriceProviderFactory;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.process.PriceProvider;
import de.tomsplayground.peanuts.domain.process.StockSplit;
import de.tomsplayground.peanuts.domain.process.Transaction;
import de.tomsplayground.peanuts.domain.reporting.investment.AnalyzerFactory;
import de.tomsplayground.util.Day;

public class InventoryTest {

	private static final String SECURITY_NAME = "Apple";
	private static final BigDecimal PRICE = new BigDecimal("99.00");
	private static final BigDecimal QUANTITY = new BigDecimal("10");
	private Account investmmentAccount;
	private Security apple;

	@Before
	public void setUp() {
		investmmentAccount = new Account("name", Currency.getInstance("EUR"), BigDecimal.ZERO,
			Account.Type.INVESTMENT, "");
		apple = new Security(SECURITY_NAME);
		InvestmentTransaction investmentTransaction1 = new InvestmentTransaction(
			Day.today(), apple, PRICE, QUANTITY, BigDecimal.ZERO,
			InvestmentTransaction.Type.BUY);
		investmmentAccount.addTransaction(investmentTransaction1);
	}

	@Test
	public void testInventory() throws Exception {
		Inventory inventory = new Inventory(investmmentAccount);

		assertEquals(1, inventory.getSecurities().size());
	}

	@Test
	public void testInventorySum() throws Exception {
		InvestmentTransaction investmentTransaction2 = new InvestmentTransaction(
			Day.today(), apple, PRICE, QUANTITY, BigDecimal.ZERO,
			InvestmentTransaction.Type.BUY);
		investmmentAccount.addTransaction(investmentTransaction2);
		Inventory inventory = new Inventory(investmmentAccount);

		assertEquals(1, inventory.getSecurities().size());
	}

	@Test
	public void testSellAll() throws Exception {
		InvestmentTransaction investmentTransaction2 = new InvestmentTransaction(
			Day.today(), apple, PRICE, QUANTITY, BigDecimal.ZERO,
			InvestmentTransaction.Type.SELL);
		investmmentAccount.addTransaction(investmentTransaction2);
		Inventory inventory = new Inventory(investmmentAccount);

		assertEquals(1, inventory.getSecurities().size());
	}

	@Test
	public void testOtherInvestmentTransactions() throws Exception {
		InvestmentTransaction expense = new InvestmentTransaction(Day.today(), apple,
			new BigDecimal("-5.00"), BigDecimal.ONE, BigDecimal.ZERO,
			InvestmentTransaction.Type.EXPENSE);
		investmmentAccount.addTransaction(expense);
		Inventory inventory = new Inventory(investmmentAccount);

		assertEquals(1, inventory.getSecurities().size());
	}

	@Test
	public void testInventoryEntry() throws Exception {
		Inventory inventory = new Inventory(investmmentAccount);

		Collection<InventoryEntry> quantityMap = inventory.getEntries();
		assertEquals(1, quantityMap.size());
		InventoryEntry entry = quantityMap.iterator().next();
		assertEquals(SECURITY_NAME, entry.getSecurity().getName());
		assertEquals(QUANTITY, entry.getQuantity());
		assertEquals(1, entry.getTransactions().size());
	}

	@Test
	public void testIntventorySetDate() {
		Inventory inventory = new Inventory(investmmentAccount);
		inventory.setDate(Day.today().addDays(-1));

		assertEquals(0, inventory.getSecurities().size());
		assertEquals(0, inventory.getEntries().size());
	}

	@Test
	public void testInventoryEntry2() throws Exception {
		InvestmentTransaction investmentTransaction2 = new InvestmentTransaction(
			Day.today(), apple, PRICE, QUANTITY, BigDecimal.ZERO,
			InvestmentTransaction.Type.SELL);
		investmmentAccount.addTransaction(investmentTransaction2);
		Inventory inventory = new Inventory(investmmentAccount);

		Collection<InventoryEntry> quantityMap = inventory.getEntries();
		assertEquals(1, quantityMap.size());
		InventoryEntry entry = quantityMap.iterator().next();
		assertEquals(SECURITY_NAME, entry.getSecurity().getName());
		assertEquals(BigDecimal.ZERO, entry.getQuantity());
		assertEquals(2, entry.getTransactions().size());
	}

	@Test
	public void testSplittedTransacions() {
		Day now = Day.today();
		Transaction transaction = new Transaction(now, BigDecimal.ZERO);
		transaction.addSplit(new BankTransaction(now, new BigDecimal("-990.00"), "??"));
		transaction.addSplit(new InvestmentTransaction(now, apple, PRICE, QUANTITY, BigDecimal.ZERO,
			InvestmentTransaction.Type.SELL));
		investmmentAccount.addTransaction(transaction);
		Inventory inventory = new Inventory(investmmentAccount);

		Collection<InventoryEntry> quantityMap = inventory.getEntries();
		assertEquals(1, quantityMap.size());
		InventoryEntry entry = quantityMap.iterator().next();
		assertEquals(SECURITY_NAME, entry.getSecurity().getName());
		assertEquals(BigDecimal.ZERO, entry.getQuantity());
		assertEquals(2, entry.getTransactions().size());
	}

	private static class SimplePriceProvider extends PriceProvider {
		SimplePriceProvider() {
			super(null);
		}
	}

	@Test
	public void testMarketValue() {
		// remove investmentTransaction1
		investmmentAccount.reset();

		final Day now = new Day(2008, 4, 3);
		// one share
		InvestmentTransaction buy = new InvestmentTransaction(now.addDays(-1), apple, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, InvestmentTransaction.Type.BUY);
		investmmentAccount.addTransaction(buy);
		// 10 shares
		buy = new InvestmentTransaction(now, apple, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, InvestmentTransaction.Type.BUY);
		investmmentAccount.addTransaction(buy);

		Inventory inventory = new Inventory(investmmentAccount, new IPriceProviderFactory() {
			@Override
			public IPriceProvider getPriceProvider(Security security) {
				SimplePriceProvider priceProvider = new SimplePriceProvider();
				priceProvider.setPrice(new Price(now.addDays(-1), new BigDecimal("11.00")));
				priceProvider.setPrice(new Price(now, new BigDecimal("12.00")));
				return priceProvider;
			}
			@Override
			public IPriceProvider getSplitAdjustedPriceProvider(Security security, List<StockSplit> stockSplits) {
				return getPriceProvider(security);
			}
		});

		inventory.setDate(now);
		Helper.assertEquals(new BigDecimal("132.00"), inventory.getMarketValue());

		inventory.setDate(now.addDays(-1));
		Helper.assertEquals(new BigDecimal("11.00"), inventory.getMarketValue());

		// incremental step from (now-1) => (now)
		inventory.setDate(now);
		Helper.assertEquals(new BigDecimal("132.00"), inventory.getMarketValue());
	}

	@Test
	public void testAnalyzed() throws Exception {
		// remove investmentTransaction1
		investmmentAccount.reset();

		final Day now = Day.today();
		InvestmentTransaction buy = new InvestmentTransaction(now, apple, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ZERO, InvestmentTransaction.Type.BUY);
		investmmentAccount.addTransaction(buy);

		Inventory inventory = new Inventory(investmmentAccount, new IPriceProviderFactory() {
			@Override
			public IPriceProvider getPriceProvider(Security security) {
				SimplePriceProvider priceProvider = new SimplePriceProvider();
				priceProvider.setPrice(new Price(now, new BigDecimal("12.00")));
				return priceProvider;
			}
			@Override
			public IPriceProvider getSplitAdjustedPriceProvider(Security security, List<StockSplit> stockSplits) {
				return getPriceProvider(security);
			}
		}, now, new AnalyzerFactory());

		Collection<InventoryEntry> entries = inventory.getEntries();
		assertEquals(1, entries.size());
		InventoryEntry entry = entries.iterator().next();
		assertEquals(0, entry.getGaining().compareTo(BigDecimal.ZERO));
		Helper.assertEquals(new BigDecimal("12"), entry.getMarketValue(now));
		Helper.assertEquals(BigDecimal.ONE, entry.getQuantity());
		Helper.assertEquals(BigDecimal.TEN, entry.getInvestedAmount());
	}

	@Test
	public void priceUpdate() throws Exception {
		// remove investmentTransaction1
		investmmentAccount.reset();

		final Day now = Day.today();
		// one share
		InvestmentTransaction buy = new InvestmentTransaction(now, apple, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ZERO, InvestmentTransaction.Type.BUY);
		investmmentAccount.addTransaction(buy);

		final SimplePriceProvider priceProvider = new SimplePriceProvider();
		priceProvider.setPrice(new Price(now, new BigDecimal("12.00")));
		Inventory inventory = new Inventory(investmmentAccount, new IPriceProviderFactory() {
			@Override
			public IPriceProvider getPriceProvider(Security security) {
				return priceProvider;
			}
			@Override
			public IPriceProvider getSplitAdjustedPriceProvider(Security security, List<StockSplit> stockSplits) {
				return getPriceProvider(security);
			}
		});

		Helper.assertEquals(new BigDecimal("12.00") ,inventory.getMarketValue());
		InventoryEntry inventoryEntry = inventory.getEntries().iterator().next();
		final PropertyChangeEvent lastEvent[] = new PropertyChangeEvent[1];
		inventory.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				lastEvent[0] = evt;
			}
		});

		priceProvider.setPrice(new Price(now, new BigDecimal("13.00")));
		Helper.assertEquals(new BigDecimal("13.00"), inventory.getMarketValue());
		assertNotNull(lastEvent[0]);
		assertEquals(inventory, lastEvent[0].getSource());
		assertEquals(inventoryEntry, lastEvent[0].getNewValue());
	}
}
