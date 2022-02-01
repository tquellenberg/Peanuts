package de.tomsplayground.peanuts.portfolio;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Sets;

import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.Account.Type;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.dividend.Dividend;
import de.tomsplayground.peanuts.domain.note.Note;
import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.domain.process.StockSplit;
import de.tomsplayground.peanuts.domain.process.TransferTransaction;
import de.tomsplayground.peanuts.domain.statistics.SecurityCategoryMapping;
import de.tomsplayground.peanuts.persistence.Persistence;
import de.tomsplayground.peanuts.persistence.xstream.PersistenceService;
import de.tomsplayground.peanuts.util.Day;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.SaveFlag;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.SecurityEvent.DividendEvent;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.Taxonomy.Visitor;
import name.abuchen.portfolio.model.TaxonomyTemplate;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

public class PortfolioExport {

	private String peanutsFilename;
	private String portfolioFilename;
	private String password;

	private final Map<Account, name.abuchen.portfolio.model.Account> accountMap = new HashMap<>();
	private final Map<Security, name.abuchen.portfolio.model.Security> securityMap = new HashMap<>();
	private final Map<name.abuchen.portfolio.model.Account, Portfolio> portfolioMap = new HashMap<>();
	private PriceProviderFactory priceProviderFactory;

	private SecurityCategoryMapping regionsMapping;
	private Taxonomy regionsTaxonomy;

	private SecurityCategoryMapping sectorMapping;
	private Taxonomy sectorTaxonomy;

	private List<SecurityCategoryMapping> securityCategoryMappings;

	public static void main(String[] args) throws IOException {
		PortfolioExport portfolioExport = new PortfolioExport();
		portfolioExport.setPeanutsFilename(args[0]);
		portfolioExport.setPortfolioFilename(args[1]);
		portfolioExport.setPassword(args[2]);
		PriceProviderFactory.setLocalPriceStorePath(args[3]);
		portfolioExport.doit();
	}

	private void doit() throws IOException {
		priceProviderFactory = PriceProviderFactory.getInstance();
		savePortfolio(convert(loadPeanuts()));
	}

	private Client convert(AccountManager accountManager) {
		securityCategoryMappings = accountManager.getSecurityCategoryMappings();
		for (SecurityCategoryMapping securityCategoryMapping : securityCategoryMappings) {
			if (securityCategoryMapping.getName().equals("Region")) {
				regionsMapping = securityCategoryMapping;
			}
			if (securityCategoryMapping.getName().equals("Sector")) {
				sectorMapping = securityCategoryMapping;
			}
		}

		Client client = new Client();
		regionsTaxonomy = TaxonomyTemplate.byId("regions-msci").build();
		sectorTaxonomy = TaxonomyTemplate.byId("industry-gics-1st-level").build();
		client.addTaxonomy(regionsTaxonomy);
		client.addTaxonomy(sectorTaxonomy);
		convertSecurities(accountManager, client);
		convertAccounts(accountManager, client);
		convertSecurityPrices(accountManager);
		
		return client;
	}

	private void convertSecurityPrices(AccountManager accountManager) {
		for (Security security : accountManager.getSecurities()) {
			name.abuchen.portfolio.model.Security s = securityMap.get(security);
			for (IPrice price : priceProviderFactory.getPriceProvider(security).getPrices()) {
				s.addPrice(new SecurityPrice(price.getDay().toLocalDate(), toPpPrice(price.getValue())));
			}
			for (StockSplit stockSplit : accountManager.getStockSplits(security)) {
				SecurityEvent event = new SecurityEvent(stockSplit.getDay().toLocalDate(),
						SecurityEvent.Type.STOCK_SPLIT, stockSplit.getTo() + ":" + stockSplit.getFrom());
				s.addEvent(event);
			}
		}
	}

	private void convertAccounts(AccountManager accountManager, Client client) {
		for (Account account : accountManager.getAccounts()) {
			name.abuchen.portfolio.model.Account a = new name.abuchen.portfolio.model.Account();
			a.setName(account.getName());
			a.setRetired(account.isDeleted());
			a.setCurrencyCode(account.getCurrency().getCurrencyCode());
			client.addAccount(a);
			if (account.getType() == Type.INVESTMENT || account.getType() == Type.COMMODITY) {
				Portfolio portfolio = new Portfolio();
				portfolio.setName(account.getName());
				portfolio.setRetired(account.isDeleted());
				portfolio.setReferenceAccount(a);
				client.addPortfolio(portfolio);
				portfolioMap.put(a, portfolio);
			}
			accountMap.put(account, a);
		}
		for (Account account : accountManager.getAccounts()) {
			System.out.println(account.getName());
			for (ITransaction tx : account.getFlatTransactions()) {
				convertTransaction(account, tx, accountManager.getFullInventory());
			}
		}
	}
	
	private long toPpQuantity(BigDecimal v) {
		return toPpPrice(v);
	}

	private long toPpAmount(BigDecimal v) {
		return v.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValue();
	}

	private long toPpPrice(BigDecimal v) {
		return v.setScale(8, RoundingMode.HALF_UP).movePointRight(8).longValue();
	}

	private void convertTransaction(Account account, ITransaction tx, Inventory inventory) {
		name.abuchen.portfolio.model.Account a = accountMap.get(account);
		String currencyCode = account.getCurrency().getCurrencyCode();
		LocalDateTime date = tx.getDay().toLocalDateTime();
		long amount = toPpAmount(tx.getAmount());
		if (tx instanceof TransferTransaction) {
			TransferTransaction tt = (TransferTransaction) tx;
			checkTransferTransaction(account, tt);
			if (amount < 0) {
				Account targetAccount = (Account) tt.getTarget();
				AccountTransferEntry transferEntry = new AccountTransferEntry(a, accountMap.get(targetAccount));
				transferEntry.setCurrencyCode(currencyCode);
				transferEntry.setAmount(-amount);
				transferEntry.setDate(date);
				if (!account.getCurrency().equals(targetAccount.getCurrency())) {
					transferEntry.getTargetTransaction().setCurrencyCode(targetAccount.getCurrency().getCurrencyCode());
					transferEntry.getTargetTransaction()
							.setAmount(toPpAmount(tt.getComplement().getAmount()));

					Transaction.Unit forex = new Transaction.Unit(Transaction.Unit.Type.GROSS_VALUE,
							Money.of(currencyCode, -amount),
							Money.of(targetAccount.getCurrency().getCurrencyCode(),
									toPpAmount(tt.getComplement().getAmount())),
							new BigDecimal((double) -amount
									/ (double) toPpAmount(tt.getComplement().getAmount())));
					transferEntry.getSourceTransaction().addUnit(forex);
				}
				transferEntry.insert();
			}
		} else {
			if (tx instanceof InvestmentTransaction) {
				InvestmentTransaction ivt = (InvestmentTransaction) tx;
				Security peanutsSecurity = ivt.getSecurity();
				name.abuchen.portfolio.model.Security security = securityMap.get(peanutsSecurity);

				if (ivt.getType() == InvestmentTransaction.Type.BUY
						|| ivt.getType() == InvestmentTransaction.Type.SELL) {
					PortfolioTransaction.Type type = null;
					if (ivt.getType() == InvestmentTransaction.Type.BUY) {
						type = PortfolioTransaction.Type.BUY;
						amount = -amount;
					} else if (ivt.getType() == InvestmentTransaction.Type.SELL) {
						type = PortfolioTransaction.Type.SELL;
					}
					BuySellEntry buySellEntry = new BuySellEntry(portfolioMap.get(a), a);
					buySellEntry.setType(type);
					buySellEntry.setAmount(amount);
					buySellEntry.setCurrencyCode(currencyCode);
					buySellEntry.setDate(date);
					buySellEntry.setSecurity(security);
					buySellEntry.setShares(toPpQuantity(ivt.getQuantity()));
					buySellEntry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
							Money.of(currencyCode, toPpAmount(ivt.getCommission()))));
					buySellEntry.insert();
				} else {
					name.abuchen.portfolio.model.AccountTransaction.Type type = AccountTransaction.Type.DEPOSIT;
					if (ivt.getType() == InvestmentTransaction.Type.EXPENSE) {
						type = AccountTransaction.Type.TAXES;
						amount = -amount;
						AccountTransaction t = new AccountTransaction(date, currencyCode, amount, security, type);
						a.addTransaction(t);
					} else if (ivt.getType() == InvestmentTransaction.Type.INCOME) {
						type = AccountTransaction.Type.DIVIDENDS;
						AccountTransaction t = new AccountTransaction(date, currencyCode, amount, security, type);
						
						inventory.setDate(tx.getDay());
						BigDecimal shares = inventory.getInventoryEntry(peanutsSecurity).getQuantity();
						t.setShares(toPpQuantity(shares));
						
						Dividend dividendDetails = findDividend(peanutsSecurity.getDividends(), tx.getDay());
						if (dividendDetails != null) {
							BigDecimal tax = dividendDetails.getTaxInDefaultCurrency();
							if (tax != null) {
								t.addUnit(new Unit(Unit.Type.TAX, Money.of(currencyCode, toPpAmount(tax))));
							}
						}
						a.addTransaction(t);
					}
				}
			} else {
				name.abuchen.portfolio.model.AccountTransaction.Type type;
				if (amount < 0) {
					type = AccountTransaction.Type.REMOVAL;
					amount = -amount;
				} else {
					type = AccountTransaction.Type.DEPOSIT;
				}
				AccountTransaction t = new AccountTransaction(date, currencyCode, amount, null, type);
				a.addTransaction(t);
			}
		}
	}
	
	private Dividend findDividend(List<Dividend> all, Day date) {
		return all.stream()
			.filter(d -> Math.abs(d.getPayDate().delta(date)) < 10)
			.findAny().orElse(null);
	}

	private void checkTransferTransaction(Account account1, TransferTransaction tt1) {
		TransferTransaction tt2 = tt1.getComplement();
		Account account2 = (Account) tt1.getTarget();
//		if (tt1.isSource() == tt2.isSource()) {
//			System.err.println(account1.getName()+ " "+tt1.getDay() + " " + account2.getName()+ " " + tt1.getAmount() + " SOURCE EQUALS");
//		}
		if (!account2.getFlatTransactions().contains(tt2)) {
			System.err.println(account1.getName() + " " + tt1.getDay() + " " + account2.getName() + " "
					+ tt1.getAmount() + " TARGET MISSING");
		}
	}

	private void convertSecurities(AccountManager accountManager, Client client) {
		for (Security security : accountManager.getSecurities()) {
			name.abuchen.portfolio.model.Security s = new name.abuchen.portfolio.model.Security();
			s.setIsin(security.getISIN());
			s.setName(security.getName());
			s.setRetired(security.isDeleted());
			StringBuilder sb = new StringBuilder();
			for (Note n : security.getNotes()) {
				sb.append(n.getText());
			}
			s.setNote(sb.toString());
			s.setWkn(security.getWKN());
			s.setTickerSymbol(security.getTicker());
			client.addSecurity(s);
			
			attachTaxonomyRegion(security, s);
			attachTaxonomySector(security, s);
			securityMap.put(security, s);
			
			for (Dividend dividend : security.getDividends()) {
				LocalDate exDate = dividend.getPayDate().toLocalDate();
				LocalDate payDate = exDate;
				Money amount = Money.of(dividend.getCurrency().getCurrencyCode(), toPpAmount(dividend.getAmountPerShare()));
				String source = "Peanuts";
				s.addEvent(new DividendEvent(exDate, payDate, amount, source));
			}
		}
	}

	private void attachTaxonomyRegion(Security security, name.abuchen.portfolio.model.Security s) {
		String category = regionsMapping.getCategory(security);
		if (category.equals("USA")) {
			category = "Vereinigte Staaten";
		}
		if (category.equals("UK")) {
			category = "Großbritannien";
		}
		if (category.equals("Singapore")) {
			category = "Singapur";
		}		
		if (category.equals("Korea")) {
			category = "Südkorea";
		}		
		String country = category;
		regionsTaxonomy.foreach(new Visitor() {
			@Override
			public void visit(Classification c) {
				if (c.getName().equals(country)) {
					c.addAssignment(new Assignment(s));
				}
			}
		});
	}

	private void attachTaxonomySector(Security security, name.abuchen.portfolio.model.Security s) {
		String category = sectorMapping.getCategory(security);
		if (category.equals("Technology")) {
			category = "Informationstechnologie";
		}
		if (category.equals("Consumer Defensive")) {
			category = "Basiskonsumgüter";
		}
		if (category.equals("Healthcare")) {
			category = "Gesundheitswesen";
		}		
		if (category.equals("Industrials")) {
			category = "Industrie";
		}		
		if (category.equals("Financial Services")) {
			category = "Finanzwesen";
		}		
		if (category.equals("Basic Materials")) {
			category = "Roh-, Hilfs- & Betriebsstoffe";
		}		
		if (category.equals("Utilities")) {
			category = "Versorgungsbetriebe";
		}		
		if (category.equals("Real Estate")) {
			category = "Immobilien";
		}		
		if (category.equals("Communication Services")) {
			category = "Kommunikationsdienste";
		}		
		if (category.equals("Energy")) {
			category = "Energie";
		}		
		if (category.equals("Consumer Cyclical")) {
			category = "Nicht-Basiskonsumgüter";
		}		
		System.out.println(category);
		String sector = category;
		sectorTaxonomy.foreach(new Visitor() {
			@Override
			public void visit(Classification c) {
				if (c.getName().equals(sector)) {
					c.addAssignment(new Assignment(s));
				}
			}
		});
	}

	private void setPeanutsFilename(String peanutsFilename) {
		this.peanutsFilename = peanutsFilename;
	}

	public void setPortfolioFilename(String portfolioFilename) {
		this.portfolioFilename = portfolioFilename;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	private void savePortfolio(Client client) throws IOException {
		ClientFactory.saveAs(client, new File(portfolioFilename), password.toCharArray(),
				Sets.newHashSet(SaveFlag.XML, SaveFlag.AES128));
	}

	private AccountManager loadPeanuts() {
		Persistence persistence = new Persistence();
		persistence.setPersistenceService(new PersistenceService());
		return persistence.read(Persistence.secureReader(new File(peanutsFilename), password));
	}

}
