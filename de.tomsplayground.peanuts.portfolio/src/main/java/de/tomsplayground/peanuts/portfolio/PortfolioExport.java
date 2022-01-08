package de.tomsplayground.peanuts.portfolio;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Sets;

import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.Account.Type;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.note.Note;
import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.domain.process.StockSplit;
import de.tomsplayground.peanuts.domain.process.TransferTransaction;
import de.tomsplayground.peanuts.persistence.Persistence;
import de.tomsplayground.peanuts.persistence.xstream.PersistenceService;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.SaveFlag;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.SecurityPrice;
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
		Client client = new Client();
		convertSecurities(accountManager, client);
		convertAccounts(accountManager, client);
		convertSecurityPrices(accountManager);
		return client;
	}

	private void convertSecurityPrices(AccountManager accountManager) {
		for (Security security : accountManager.getSecurities()) {
			name.abuchen.portfolio.model.Security s = securityMap.get(security);
			for (IPrice price : priceProviderFactory.getPriceProvider(security).getPrices()) {
				s.addPrice(new SecurityPrice(price.getDay().toLocalDate(), price.getValue().movePointRight(8).longValue()));
			}
			for (StockSplit stockSplit : accountManager.getStockSplits(security)) {
		        SecurityEvent event = new SecurityEvent(stockSplit.getDay().toLocalDate(), SecurityEvent.Type.STOCK_SPLIT,
		        	stockSplit.getTo() + ":" + stockSplit.getFrom());
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
				convertTransaction(account, tx);
			}
		}
	}

	private void convertTransaction(Account account, ITransaction tx) {
		name.abuchen.portfolio.model.Account a = accountMap.get(account);
		String currencyCode = account.getCurrency().getCurrencyCode();
		LocalDateTime date = tx.getDay().toLocalDateTime();
		long amount = tx.getAmount().movePointRight(2).longValue();
		if (tx instanceof TransferTransaction) {
			TransferTransaction tt = (TransferTransaction)tx;
			checkTransferTransaction(account, tt);
			if (amount < 0) {
				Account targetAccount = (Account) tt.getTarget();
				AccountTransferEntry transferEntry = new AccountTransferEntry(a, accountMap.get(targetAccount));
				transferEntry.setCurrencyCode(currencyCode);
				transferEntry.setAmount(-amount);
				transferEntry.setDate(date);
				if (! account.getCurrency().equals(targetAccount.getCurrency())) {
					transferEntry.getTargetTransaction().setCurrencyCode(targetAccount.getCurrency().getCurrencyCode());
					transferEntry.getTargetTransaction().setAmount(tt.getComplement().getAmount().movePointRight(2).longValue());

		            Transaction.Unit forex = new Transaction.Unit(Transaction.Unit.Type.GROSS_VALUE,
                            Money.of(currencyCode, -amount),
                            Money.of(targetAccount.getCurrency().getCurrencyCode(), tt.getComplement().getAmount().movePointRight(2).longValue()),
                            new BigDecimal((double)-amount / (double)tt.getComplement().getAmount().movePointRight(2).longValue()));
		            transferEntry.getSourceTransaction().addUnit(forex);
				}
				transferEntry.insert();
			}
		} else {
			if (tx instanceof InvestmentTransaction) {
				InvestmentTransaction ivt = (InvestmentTransaction)tx;
				name.abuchen.portfolio.model.Security security = securityMap.get(ivt.getSecurity());

				if (ivt.getType() == InvestmentTransaction.Type.BUY || ivt.getType() == InvestmentTransaction.Type.SELL) {
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
					buySellEntry.setShares(ivt.getQuantity().movePointRight(8).longValue());
					buySellEntry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, Money.of(currencyCode, ivt.getCommission().movePointRight(2).longValue())));
					buySellEntry.insert();
				} else {
					name.abuchen.portfolio.model.AccountTransaction.Type type = AccountTransaction.Type.DEPOSIT;
					if (ivt.getType() == InvestmentTransaction.Type.EXPENSE) {
						type = AccountTransaction.Type.TAXES;
						amount = -amount;
					} else if (ivt.getType() == InvestmentTransaction.Type.INCOME) {
						type = AccountTransaction.Type.DIVIDENDS;
					}
					AccountTransaction t = new AccountTransaction(date, currencyCode, amount, security, type);
					a.addTransaction(t);
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

	private void checkTransferTransaction(Account account1, TransferTransaction tt1) {
		TransferTransaction tt2 = tt1.getComplement();
		Account account2 = (Account) tt1.getTarget();
//		if (tt1.isSource() == tt2.isSource()) {
//			System.err.println(account1.getName()+ " "+tt1.getDay() + " " + account2.getName()+ " " + tt1.getAmount() + " SOURCE EQUALS");
//		}
		if (! account2.getFlatTransactions().contains(tt2)) {
			System.err.println(account1.getName()+ " "+tt1.getDay() + " " + account2.getName()+ " " + tt1.getAmount() + " TARGET MISSING");
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
			s.setTickerSymbol(security.getMorningstarSymbol());
			client.addSecurity(s);
			securityMap.put(security, s);
		}
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
		ClientFactory.saveAs(client, new File(portfolioFilename), password.toCharArray(), Sets.newHashSet(SaveFlag.AES256));
	}

	private AccountManager loadPeanuts() {
		Persistence persistence = new Persistence();
		persistence.setPersistenceService(new PersistenceService());
		return persistence.read(Persistence.secureReader(new File(peanutsFilename), password));
	}

}
