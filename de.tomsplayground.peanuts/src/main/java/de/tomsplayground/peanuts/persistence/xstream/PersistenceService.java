package de.tomsplayground.peanuts.persistence.xstream;

import java.io.StringWriter;

import com.google.common.collect.ImmutableList;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.extended.ISO8601GregorianCalendarConverter;
import com.thoughtworks.xstream.io.xml.CompactWriter;

import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Category;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.calendar.SecurityCalendarEntry;
import de.tomsplayground.peanuts.domain.fundamental.FundamentalData;
import de.tomsplayground.peanuts.domain.note.Note;
import de.tomsplayground.peanuts.domain.process.BankTransaction;
import de.tomsplayground.peanuts.domain.process.Credit;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
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

	private final XStream stream;

	public PersistenceService() {
		stream = new XStream();
		stream.aliasType("immutableList", ImmutableList.class);
		stream.setMode(XStream.ID_REFERENCES);
		stream.addDefaultImplementation(DummyImmutableList.class, ImmutableList.class);
		stream.registerConverter(new ImmutableListConverter(stream.getMapper()));
		stream.registerConverter(new ISO8601GregorianCalendarConverter());
		stream.processAnnotations(new Class[]{
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
			Note.class,
			WatchlistConfiguration.class,
			CategoryFilter.class
		});
	}

	@Override
	public String write(AccountManager accountManager) {
		StringWriter stringWriter = new StringWriter();
		stream.marshal(accountManager, new CompactWriter(stringWriter));
		return stringWriter.toString();
	}

	@Override
	public AccountManager readAccountManager(String xml) {
		return (AccountManager) stream.fromXML(xml);
	}

}
