package de.tomsplayground.peanuts.domain.reporting.transaction;

import static org.junit.Assert.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.tomsplayground.peanuts.Helper;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.IPriceProviderFactory;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.process.PriceProvider;
import de.tomsplayground.peanuts.domain.process.StockSplit;
import de.tomsplayground.peanuts.domain.process.Transaction;
import de.tomsplayground.util.Day;

public class TimeIntervalReportTest {

	private Account investmmentAccount;

	@Before
	public void setUp() {
		AccountManager accountManager = new AccountManager();
		investmmentAccount = accountManager.getOrCreateAccount("name", Account.Type.INVESTMENT);
	}

	@Test
	public void testValues() throws Exception {
		Day date = new Day().addDays(-3);
		investmmentAccount.addTransaction(new Transaction(date, BigDecimal.TEN));
		TimeIntervalReport timeIntervalReport = new TimeIntervalReport(investmmentAccount,
			TimeIntervalReport.Interval.DAY);

		List<BigDecimal> values = timeIntervalReport.getValues();
		assertEquals(4, values.size());
	}

	@Test
	public void testMonth() throws Exception {
		Day date = new Day().addMonth(-3);
		investmmentAccount.addTransaction(new Transaction(date, BigDecimal.TEN));
		date = new Day().addMonth(-1);
		investmmentAccount.addTransaction(new Transaction(date, BigDecimal.ONE));
		TimeIntervalReport timeIntervalReport = new TimeIntervalReport(investmmentAccount,
			TimeIntervalReport.Interval.MONTH);

		List<BigDecimal> values = timeIntervalReport.getValues();
		DateIterator dateIterator = timeIntervalReport.dateIterator();
		Day d = dateIterator.next();
		date = new Day().addMonth(-3);
		assertEquals(new Day(date.year, date.month, 1), d);
		assertEquals(0, values.get(0).compareTo(BigDecimal.TEN));
		d = dateIterator.next();
		date = new Day().addMonth(-2);
		assertEquals(new Day(date.year, date.month, 1), d);
		assertEquals(0, values.get(1).compareTo(BigDecimal.ZERO));
		d = dateIterator.next();
		date = new Day().addMonth(-1);
		assertEquals(new Day(date.year, date.month, 1), d);
		assertEquals(0, values.get(2).compareTo(BigDecimal.ONE));
		d = dateIterator.next();
		date = new Day();
		assertEquals(new Day(date.year, date.month, 1), d);
		assertEquals(0, values.get(3).compareTo(BigDecimal.ZERO));
		assertFalse(dateIterator.hasNext());
	}

	@Test
	public void testFutureTransaction() throws Exception {
		Day date = new Day().addDays(-3);
		investmmentAccount.addTransaction(new Transaction(date, BigDecimal.TEN));
		date = new Day().addDays(+3);
		investmmentAccount.addTransaction(new Transaction(date, BigDecimal.TEN));
		TimeIntervalReport timeIntervalReport = new TimeIntervalReport(investmmentAccount,
			TimeIntervalReport.Interval.DAY);

		List<BigDecimal> values = timeIntervalReport.getValues();
		assertEquals(7, values.size());
	}

	@Test
	public void testEmpty() throws Exception {
		TimeIntervalReport timeIntervalReport = new TimeIntervalReport(investmmentAccount,
			TimeIntervalReport.Interval.MONTH);
		List<BigDecimal> values = timeIntervalReport.getValues();
		assertEquals(0, values.size());
	}

	private static class SimplePriceProvider extends PriceProvider {
		SimplePriceProvider() {
			super(null);
		}
		@Override
		public String getName() {
			return "test";
		}

		public void addPrice(Price price) {
			setPrice(price);
		}
	}

	@Test
	public void inventoryValues() throws Exception {
		Day now = new Day();
		investmmentAccount.addTransaction(new InvestmentTransaction(now, new Security("Apple"), BigDecimal.ONE,
			BigDecimal.ONE, BigDecimal.ZERO, InvestmentTransaction.Type.BUY));
		final SimplePriceProvider priceProvider = new SimplePriceProvider();
		priceProvider.addPrice(new Price(now, BigDecimal.TEN));
		TimeIntervalReport timeIntervalReport = new TimeIntervalReport(investmmentAccount, TimeIntervalReport.Interval.MONTH, new IPriceProviderFactory(){
			@Override
			public IPriceProvider getPriceProvider(Security security) {
				return priceProvider;
			}
			@Override
			public IPriceProvider getAdjustedPriceProvider(Security security, List<StockSplit> stockSplits) {
				return getPriceProvider(security);
			}
		});

		List<BigDecimal> inventoryValues = timeIntervalReport.getInventoryValues();
		assertEquals(1, inventoryValues.size());
		Helper.assertEquals(BigDecimal.TEN, inventoryValues.get(0));

		priceProvider.setPrice(new Price(now, new BigDecimal("11")));
		inventoryValues = timeIntervalReport.getInventoryValues();
		assertEquals(1, inventoryValues.size());
		Helper.assertEquals(new BigDecimal("11"), inventoryValues.get(0));
	}

	@Test
	public void futureTransactionInventoryValues() throws Exception {
		final SimplePriceProvider priceProvider = new SimplePriceProvider();
		// Today
		Day today = new Day();
		investmmentAccount.addTransaction(new InvestmentTransaction(today, new Security("Apple"), BigDecimal.ONE,
			BigDecimal.ONE, BigDecimal.ZERO, InvestmentTransaction.Type.BUY));
		priceProvider.addPrice(new Price(today, BigDecimal.TEN));
		// Tomorrow
		Day tomorrow = new Day().addDays(1);
		investmmentAccount.addTransaction(new Transaction(tomorrow, BigDecimal.ONE));
		priceProvider.addPrice(new Price(tomorrow, new BigDecimal("11")));
		TimeIntervalReport timeIntervalReport = new TimeIntervalReport(investmmentAccount,
			TimeIntervalReport.Interval.DAY, new IPriceProviderFactory(){
			@Override
			public IPriceProvider getPriceProvider(Security security) {
				return priceProvider;
			}
			@Override
			public IPriceProvider getAdjustedPriceProvider(Security security, List<StockSplit> stockSplits) {
				return getPriceProvider(security);
			}
		});

		List<BigDecimal> inventoryValues = timeIntervalReport.getInventoryValues();
		assertEquals(2, inventoryValues.size());
		Helper.assertEquals(BigDecimal.TEN, inventoryValues.get(0));
		Helper.assertEquals(new BigDecimal("11"), inventoryValues.get(1));
	}

	@Test
	public void propertyChanged() throws Exception {
		Day now = new Day();
		investmmentAccount.addTransaction(new InvestmentTransaction(now, new Security("Apple"), BigDecimal.ONE,
			BigDecimal.ONE, BigDecimal.ZERO, InvestmentTransaction.Type.BUY));
		final SimplePriceProvider priceProvider = new SimplePriceProvider();
		priceProvider.addPrice(new Price(now, BigDecimal.TEN));
		TimeIntervalReport timeIntervalReport = new TimeIntervalReport(investmmentAccount, TimeIntervalReport.Interval.MONTH, new IPriceProviderFactory(){
			@Override
			public IPriceProvider getPriceProvider(Security security) {
				return priceProvider;
			}
			@Override
			public IPriceProvider getAdjustedPriceProvider(Security security, List<StockSplit> stockSplits) {
				return getPriceProvider(security);
			}
		});
		final PropertyChangeEvent lastEvent[] = new PropertyChangeEvent[1];
		timeIntervalReport.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				lastEvent[0] = evt;
			}
		});
		priceProvider.setPrice(new Price(now, new BigDecimal("11")));

		assertNotNull(lastEvent[0]);
		assertEquals("values", lastEvent[0].getPropertyName());
	}
}
