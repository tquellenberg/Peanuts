package de.tomsplayground.peanuts.persistence.xstream;

import java.io.Writer;

import com.google.common.collect.ImmutableList;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.extended.ISO8601GregorianCalendarConverter;
import com.thoughtworks.xstream.io.xml.CompactWriter;

import de.tomsplayground.peanuts.domain.alarm.SecurityAlarm;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Category;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.calendar.SecurityCalendarEntry;
import de.tomsplayground.peanuts.domain.comparision.Comparison;
import de.tomsplayground.peanuts.domain.dividend.Dividend;
import de.tomsplayground.peanuts.domain.fundamental.FundamentalData;
import de.tomsplayground.peanuts.domain.note.Note;
import de.tomsplayground.peanuts.domain.process.BankTransaction;
import de.tomsplayground.peanuts.domain.process.Credit;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.domain.process.NoTrailingStrategy;
import de.tomsplayground.peanuts.domain.process.PercentTrailingStrategy;
import de.tomsplayground.peanuts.domain.process.SavedTransaction;
import de.tomsplayground.peanuts.domain.process.StockSplit;
import de.tomsplayground.peanuts.domain.process.StopLoss;
import de.tomsplayground.peanuts.domain.process.Transaction;
import de.tomsplayground.peanuts.domain.process.TransferTransaction;
import de.tomsplayground.peanuts.domain.query.CategoryQuery;
import de.tomsplayground.peanuts.domain.query.DateQuery;
import de.tomsplayground.peanuts.domain.reporting.forecast.Forecast;
import de.tomsplayground.peanuts.domain.reporting.transaction.Report;
import de.tomsplayground.peanuts.domain.statistics.SecurityCategoryMapping;
import de.tomsplayground.peanuts.domain.watchlist.CategoryFilter;
import de.tomsplayground.peanuts.domain.watchlist.WatchlistConfiguration;
import de.tomsplayground.peanuts.persistence.IPersistenceService;

public class PersistenceService implements IPersistenceService {

	private static final Class<?>[] PERSISTENCE_TYPES = new Class[]{
		Account.class,
		AccountManager.class,
		Transaction.class,
		TransferTransaction.class,
		InvestmentTransaction.class,
		Category.class,
		BankTransaction.class,
		Security.class,
		FundamentalData.class,
		Report.class,
		Forecast.class,
		DateQuery.class,
		CategoryQuery.class,
		Credit.class,
		StockSplit.class,
		SecurityCategoryMapping.class,
		StopLoss.class,
		SecurityCalendarEntry.class,
		Dividend.class,
		Note.class,
		WatchlistConfiguration.class,
		CategoryFilter.class,
		SecurityAlarm.class,
		SavedTransaction.class,
		NoTrailingStrategy.class,
		PercentTrailingStrategy.class,
		Comparison.class
	};

	private final XStream xstream;

	public PersistenceService() {
		xstream = new XStream();
		xstream.aliasType("immutableList", ImmutableList.class);
		xstream.setMode(XStream.ID_REFERENCES);
		xstream.allowTypes(PERSISTENCE_TYPES);
		// Additional classes
		// TODO: replace by simple types
		xstream.allowTypes(new String[] {
			"de.tomsplayground.peanuts.domain.reporting.transaction.Report$1",
			"com.google.common.collect.ImmutableList$SerializedForm"});
		xstream.addDefaultImplementation(DummyImmutableList.class, ImmutableList.class);
		xstream.registerConverter(new ImmutableListConverter(xstream.getMapper()));
		xstream.registerConverter(new ISO8601GregorianCalendarConverter());
		xstream.processAnnotations(PERSISTENCE_TYPES);
		xstream.ignoreUnknownElements();
	}

	@Override
	public void write(AccountManager accountManager, Writer writer) {
		xstream.marshal(accountManager, new CompactWriter(writer));
	}

	@Override
	public AccountManager readAccountManager(String xml) {
		return (AccountManager) xstream.fromXML(xml);
	}

}
