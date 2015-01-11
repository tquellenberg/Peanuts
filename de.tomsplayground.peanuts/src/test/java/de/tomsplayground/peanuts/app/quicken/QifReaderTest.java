package de.tomsplayground.peanuts.app.quicken;


import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import junit.framework.TestCase;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Category;
import de.tomsplayground.peanuts.domain.base.CurrencyManager;
import de.tomsplayground.peanuts.domain.process.BankTransaction;
import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.domain.process.Transaction;
import de.tomsplayground.peanuts.domain.process.Transfer;
import de.tomsplayground.peanuts.domain.process.TransferTransaction;
import de.tomsplayground.util.Day;

public class QifReaderTest extends TestCase {

	private static final String D_MARK_ACCOUNT = "D. Mark Account";
	private static final String TEST_ACCOUNT_NAME = "testAccount";
	private static final String TEST_ACCOUNT2_NAME = "testAccount2";

	private QifReader qifReader;
	private AccountManager accountManager;
	private Account bankAccount;
	private Account investmentAccount;

	@Override
	public void setUp() {
		qifReader = new QifReader();
		accountManager = new AccountManager();
		bankAccount = accountManager.getOrCreateAccount(TEST_ACCOUNT_NAME, Account.Type.BANK);
		investmentAccount = accountManager.getOrCreateAccount(TEST_ACCOUNT2_NAME,
			Account.Type.INVESTMENT);
		qifReader.setAccountManager(accountManager);
	}

	public void testRead() throws IOException {
		qifReader.read(new InputStreamReader(QifReaderTest.class.getResourceAsStream("/test.QIF")));

		assertEquals(
			4,
			accountManager.getOrCreateAccount("Advance", Account.Type.BANK).getTransactions().size());
	}

	public void testEncoding() throws IOException {
		qifReader.read(new InputStreamReader(QifReaderTest.class.getResourceAsStream("/test.QIF"),
			"ISO-8859-1"));

		ITransaction transaction = accountManager.getOrCreateAccount("Advance", Account.Type.BANK).getTransactions().get(
			0);
		assertEquals("Eröffnungssaldo", ((BankTransaction) transaction).getLabel());
	}

	public void testAmount() {
		assertEquals(new BigDecimal("9280.00"), qifReader.readAmount("9,280.00"));
		assertEquals(new BigDecimal("63.50"), qifReader.readAmount("63,50"));
	}

	public void testReadAccount() {
		List<String> block = new ArrayList<String>();
		block.add("NAdvance");
		block.add("DKontonr. 1357471608");
		block.add("TBank");
		Account account = qifReader.readAccount(block);

		assertEquals("Advance", account.getName());
	}

	public void testReadTransaction() {
		List<String> block = new ArrayList<String>();
		block.add("D8.6.98");
		block.add("U9,280.00");
		block.add("T9,280.00");
		block.add("POxmox");
		block.add("MJuli 98");
		block.add("LGehalt");
		BankTransaction trans = (BankTransaction) qifReader.readTransaction(block, bankAccount);

		assertEquals(new Day(1998, 7, 6), trans.getDay());
		assertEquals(new BigDecimal("9280.00"), trans.getAmount());
		Category cat = accountManager.getOrCreateCategory("Gehalt");
		assertEquals(cat, trans.getCategory());
		assertEquals("Oxmox", trans.getLabel());
		assertEquals("Juli 98", trans.getMemo());
	}

	public void testReadTransferTransaction() {
		List<String> block = new ArrayList<String>();
		block.add("D8.6.98");
		block.add("U9,280.00");
		block.add("T9,280.00");
		block.add("POxmox");
		block.add("MJuli 98");
		block.add("L[Forderungen]");
		TransferTransaction trans = (TransferTransaction) qifReader.readTransaction(block,
			bankAccount);

		assertEquals(new Day(1998, 7, 6), trans.getDay());
		assertEquals(new BigDecimal("9280.00"), trans.getAmount());
		assertEquals("Oxmox", trans.getLabel());
		assertEquals("Juli 98", trans.getMemo());
		Account forderungen = accountManager.getOrCreateAccount("Forderungen", Account.Type.UNKNOWN);
		assertEquals(new BigDecimal("-9280.00"), forderungen.getBalance());
		assertEquals(forderungen, trans.getTarget());
		assertEquals(1, forderungen.getTransactions().size());
		TransferTransaction transferTransaction = (TransferTransaction) forderungen.getTransactions().get(
			0);
		assertEquals(bankAccount, transferTransaction.getTarget());
		assertEquals(trans.getDay(), transferTransaction.getDay());
		assertEquals(trans.getMemo(), transferTransaction.getMemo());
		assertEquals(trans.getLabel(), transferTransaction.getLabel());
	}

	public void testReadTransferWithDifferentCurrencies() throws Exception {
		Account account2 = accountManager.getOrCreateAccount(D_MARK_ACCOUNT, Account.Type.BANK);
		account2.setCurrency(Currency.getInstance("DEM"));

		List<String> block = new ArrayList<String>();
		block.add("D8.6.98");
		block.add("U9,280.00");
		block.add("T9,280.00");
		block.add("POxmox");
		block.add("MJuli 98");
		block.add("L[" + D_MARK_ACCOUNT + "]");
		qifReader.readTransaction(block, bankAccount);

		assertEquals((new BigDecimal("-9280.00")).multiply(CurrencyManager.DM_EURO).setScale(2,
			RoundingMode.HALF_EVEN), account2.getBalance());
	}

	public void testReadTransferTransactionEqualAccount() {
		List<String> block = new ArrayList<String>();
		block.add("D8.6.98");
		block.add("U9,280.00");
		block.add("T9,280.00");
		block.add("POxmox");
		block.add("MJuli 98");
		block.add("L[" + TEST_ACCOUNT_NAME + "]");
		qifReader.readTransaction(block, bankAccount);

		assertEquals(1, bankAccount.getTransactions().size());
		assertEquals(new BigDecimal("9280.00"), bankAccount.getBalance());
	}

	public void testReadTransactionWithComplexCategory() {
		List<String> block = new ArrayList<String>();
		block.add("D8.6.98");
		block.add("U9,280.00");
		block.add("T9,280.00");
		block.add("POxmox");
		block.add("MJuli 98");
		block.add("LSteuer:EkSt");
		BankTransaction trans = (BankTransaction) qifReader.readTransaction(block, bankAccount);

		assertEquals("EkSt", trans.getCategory().getName());
		assertEquals("Steuer", trans.getCategory().getParent().getName());
	}

	public void testReadInvestmentTransaction() {
		List<String> block = new ArrayList<String>();
		block.add("D6.21.00");
		block.add("NKauf");
		block.add("YDeutsche Telekom");
		block.add("I63,50");
		block.add("Q50");
		block.add("C*");
		block.add("U3,186.75");
		block.add("T3,186.75");
		block.add("O11.75");
		InvestmentTransaction trans = (InvestmentTransaction) qifReader.readInvestmentTransaction(
			block, investmentAccount);

		assertEquals(new Day(2000, 5, 21), trans.getDay());
		assertEquals(new BigDecimal("50"), trans.getQuantity());
		assertEquals("Deutsche Telekom", trans.getSecurity().getName());
		assertEquals(new BigDecimal("63.50"), trans.getPrice());
		assertEquals(new BigDecimal("-3186.75"), trans.getAmount());
		assertEquals(new BigDecimal("11.75"), trans.getCommission());
		assertEquals(InvestmentTransaction.Type.BUY, trans.getType());

		assertEquals(1, accountManager.getSecurities().size());
		assertEquals("Deutsche Telekom", accountManager.getSecurities().iterator().next().getName());
		assertEquals(new BigDecimal("-3186.75"), investmentAccount.getBalance());
	}

	public void testReadInvestmentTransactionTransfer() {
		List<String> block = new ArrayList<String>();
		block.add("D10.1.05");
		block.add("NKauf");
		block.add("YDEKA-INTERNET CF - (ANTEILE)");
		block.add("I8,32");
		block.add("Q12,019231");
		block.add("U100.00");
		block.add("T100.00");
		block.add("L[Fondsparkonto-Bank]");
		block.add("$100.00");
		Transaction trans = qifReader.readInvestmentTransaction(block, investmentAccount);

		assertEquals(0, trans.getAmount().compareTo(BigDecimal.ZERO));
		assertEquals(0, investmentAccount.getBalance().compareTo(BigDecimal.ZERO));
		assertEquals(2, trans.getSplits().size());
		InvestmentTransaction investTrans = (InvestmentTransaction) trans.getSplits().get(1);
		assertEquals(new BigDecimal("-100.00"), investTrans.getAmount());

		Account transferAccount = accountManager.getOrCreateAccount("Fondsparkonto-Bank",
			Account.Type.UNKNOWN);
		TransferTransaction transferTrans = (TransferTransaction) transferAccount.getTransactions().get(
			0);
		assertEquals(new BigDecimal("-100.00"), transferTrans.getAmount());
		assertFalse(transferTrans.isSource());
	}

	public void testReadQuantity() {
		BigDecimal q = qifReader.readQuantity("0,274");
		assertEquals(new BigDecimal("0.274"), q);
		q = qifReader.readQuantity("1.000");
		assertEquals(new BigDecimal("1000"), q);
	}

	public void testReadInvestmentTransactionSell() {
		List<String> block = new ArrayList<String>();
		block.add("D3.10.06");
		block.add("NVerkaufX");
		block.add("YSAP");
		block.add("I173,50");
		block.add("Q25");
		block.add("U4,337.50");
		block.add("T4,337.50");
		block.add("MSchätzpreis 10.03.06");
		block.add("L[Wertpapier-Depot-Bank]");
		block.add("$4,337.50");
		InvestmentTransaction trans = (InvestmentTransaction) qifReader.readInvestmentTransaction(
			block, investmentAccount);

		assertEquals(new BigDecimal("4337.50"), trans.getAmount());
		assertEquals(InvestmentTransaction.Type.SELL, trans.getType());
		Account transferAccount = accountManager.getOrCreateAccount("Wertpapier-Depot-Bank",
			Account.Type.UNKNOWN);
		assertEquals(0, transferAccount.getTransactions().size());
		assertEquals(new BigDecimal("4337.50"), investmentAccount.getBalance());
	}

	public void testReadDividende() {
		List<String> block = new ArrayList<String>();
		block.add("D11.17.97");
		block.add("NDiv");
		block.add("YInvesta");
		block.add("U101.58");
		block.add("T101.58");

		InvestmentTransaction trans = (InvestmentTransaction) qifReader.readInvestmentTransaction(
			block, investmentAccount);
		assertEquals(new BigDecimal("101.58"), trans.getAmount());
		assertEquals("Investa", trans.getSecurity().getName());
		assertEquals(InvestmentTransaction.Type.INCOME, trans.getType());
		assertEquals("Div", trans.getCategory().getName());
	}

	public void testReadTransferTransactionInInvestment() {
		List<String> block = new ArrayList<String>();
		block.add("D9.17.01");
		block.add("NXAus");
		block.add("C*");
		block.add("U3,000.00");
		block.add("T3,000.00");
		block.add("L[Wertpapier-Depot-Bank]");
		block.add("$3,000.00");
		TransferTransaction trans = (TransferTransaction) qifReader.readTransaction(block,
			investmentAccount);

		assertEquals(new BigDecimal("-3000.00"), trans.getAmount());
		Account transferAccount = accountManager.getOrCreateAccount("Wertpapier-Depot-Bank",
			Account.Type.UNKNOWN);
		assertEquals(1, transferAccount.getTransactions().size());
		assertEquals(new BigDecimal("3000.00"), transferAccount.getBalance());
		assertEquals(new BigDecimal("-3000.00"), investmentAccount.getBalance());
	}

	public void testReadTransferTransactionInInvestmentExistingTransfer() {
		Account transferAccount = accountManager.getOrCreateAccount("Wertpapier-Depot-Bank",
			Account.Type.UNKNOWN);
		Day date = new Day(2001, 8, 17);
		Transfer transfer = new Transfer(transferAccount, investmentAccount, new BigDecimal(
			"-3000.00"), date);
		transferAccount.addTransaction(transfer.getTransferFrom());
		investmentAccount.addTransaction(transfer.getTransferTo());

		List<String> block = new ArrayList<String>();
		block.add("D9.17.01");
		block.add("NXAus");
		block.add("C*");
		block.add("U3,000.00");
		block.add("T3,000.00");
		block.add("L[Wertpapier-Depot-Bank]");
		block.add("$3,000.00");
		qifReader.readTransaction(block, investmentAccount);
		qifReader.resolveComplementTransferTransactions();

		assertEquals(1, investmentAccount.getTransactions().size());
		assertEquals(new BigDecimal("-3000.00"), investmentAccount.getBalance());
		assertEquals(1, transferAccount.getTransactions().size());
		assertEquals(new BigDecimal("3000.00"), transferAccount.getBalance());
		assertEquals(new BigDecimal("-3000.00"), investmentAccount.getBalance());
	}

	public void testReadTransferTransactionInInvestmentExistingTransfer2() {
		Account account1 = accountManager.getOrCreateAccount("Wertpapier-Depot-Bank",
			Account.Type.BANK);
		account1.setCurrency(Currency.getInstance("DEM"));

		Account transferAccount = accountManager.getOrCreateAccount("Wertpapier-Depot",
			Account.Type.INVESTMENT);

		Day date = new Day(2001, 8, 17);
		Transfer transfer = new Transfer(account1, transferAccount, new BigDecimal("-5867.49"),
			date);
		account1.addTransaction(transfer.getTransferFrom());
		transferAccount.addTransaction(transfer.getTransferTo());

		List<String> block = new ArrayList<String>();
		block.add("D9.17.01");
		block.add("NXAus");
		block.add("C*");
		block.add("U3,000.00");
		block.add("T3,000.00");
		block.add("L[Wertpapier-Depot-Bank]");
		block.add("$3,000.00");
		qifReader.readTransaction(block, transferAccount);
		qifReader.resolveComplementTransferTransactions();

		assertEquals(1, account1.getTransactions().size());
		assertEquals(new BigDecimal("5867.49"), account1.getBalance());
		assertEquals(1, transferAccount.getTransactions().size());
		assertEquals(new BigDecimal("-3000.00"), transferAccount.getBalance());
	}

	public void testReadSingleSplit() {
		List<String> block = new ArrayList<String>();
		block.add("SGehalt");
		block.add("ELohn und Gehalt");
		block.add("$2,000.00");
		Transaction split = qifReader.readSplit(block, bankAccount, new Day());

		assertEquals("Lohn und Gehalt", split.getMemo());
		assertEquals("Gehalt", split.getCategory().getName());
		assertEquals(new BigDecimal("2000.00"), split.getAmount());
	}

	public void testReadSingleSplits() {
		List<String> block = new ArrayList<String>();
		block.add("SGehalt");
		block.add("ELohn und Gehalt");
		block.add("$2,000.00");
		block.add("SSteuer:EkSt");
		block.add("$176.64");
		List<Transaction> splits = qifReader.readSplits(block, bankAccount, new Day());

		assertEquals(2, splits.size());
		assertEquals(new BigDecimal("176.64"), splits.get(1).getAmount());
		assertEquals("Gehalt", splits.get(0).getCategory().getName());
		assertEquals("EkSt", splits.get(1).getCategory().getName());
		assertEquals("Steuer", splits.get(1).getCategory().getParent().getName());
	}

	public void testReadSplit() {
		List<String> block = new ArrayList<String>();
		block.add("D10.28.04");
		block.add("U1,851.03");
		block.add("T1,851.03");
		block.add("Pfirma");
		block.add("Mfirma");
		block.add("LGehalt");

		block.add("SGehalt");
		block.add("ELohn und Gehalt");
		block.add("$3,000.00");

		block.add("SAnd. Einkommen");
		block.add("$276.64");

		block.add("SAnd. Einkommen");
		block.add("$40.00");

		block.add("SSteuer:EkSt");
		block.add("$-1,089.08");

		block.add("SSteuer:Andere Steuer");
		block.add("EVerschiedene Steuern");
		block.add("$-59.89");

		block.add("SFahrzeuge");
		block.add("$-276.64");

		block.add("S[Activest]");
		block.add("EÜbertragen nach Acti");
		block.add("$-40.00");

		Transaction trans = qifReader.readTransaction(block, bankAccount);
		Day date = trans.getDay();
		assertEquals(new Day(2004,9,28), date);
		List<ITransaction> splits = trans.getSplits();
		assertEquals(7, splits.size());
		BigDecimal sum = BigDecimal.ZERO;
		for (ITransaction split : splits) {
			sum = sum.add(split.getAmount());
			assertEquals(date, split.getDay());
		}
		assertEquals(trans.getAmount(), sum);
		TransferTransaction transferTrans = (TransferTransaction) splits.get(6);
		Account transferAccount = accountManager.getOrCreateAccount("Activest",
			Account.Type.UNKNOWN);
		assertEquals(transferAccount, transferTrans.getTarget());
		assertEquals(trans.getDay(), transferTrans.getDay());
		assertTrue(transferTrans.isSource());
		TransferTransaction complementTransaction = (TransferTransaction) transferAccount.getTransactions().get(
			0);
		assertEquals(bankAccount, complementTransaction.getTarget());
		assertEquals(transferTrans.getDay(), complementTransaction.getDay());
		assertEquals(transferTrans.getAmount().negate(), complementTransaction.getAmount());
	}

	public void testReadTransferSplit() {
		List<String> block = new ArrayList<String>();
		block.add("D2.1.05");
		block.add("U-322.10");
		block.add("T-322.10");
		block.add("N11");
		block.add("PAutohaus Opel");
		block.add("MAbzahlung Kredit Opel Astra");
		block.add("L[Opel Astra Kredit]");
		block.add("S[Opel Astra Kredit]");
		block.add("$-238.77");
		block.add("SZinszahlung");
		block.add("$-83.33");
		Transaction trans = qifReader.readTransaction(block, bankAccount);

		assertEquals(2, trans.getSplits().size());
		assertEquals(new Day(2005,1,1), trans.getSplits().get(0).getDay());
		assertEquals(1, bankAccount.getTransactions().size());
		Account transferAccount = accountManager.getOrCreateAccount("Opel Astra Kredit",
			Account.Type.UNKNOWN);
		assertEquals(1, transferAccount.getTransactions().size());
		TransferTransaction transferTrans = (TransferTransaction) transferAccount.getTransactions().get(
			0);
		assertEquals(new BigDecimal("238.77"), transferTrans.getAmount());
	}

	public void testReadMwStSplit() {
		List<String> block = new ArrayList<String>();
		block.add("D3.25.96");
		block.add("U2,300.00");
		block.add("T2,300.00");
		block.add("C*");
		block.add("N2");
		block.add("PWELA electronic GmbH");
		block.add("MSCO");
		block.add("LGehalt");
		block.add("SGehalt");
		block.add("$2,000.00");
		block.add("S[Mehrwertsteuer]/_VATCode_N_O");
		block.add("$300.00");
		Transaction trans = qifReader.readTransaction(block, bankAccount);

		assertEquals(2, trans.getSplits().size());
		assertEquals("Gehalt", trans.getSplits().get(0).getCategory().getName());
//		assertEquals("Mehrwertsteuer", trans.getSplits().get(1).getCategory().getName());
		Account mwstAccount = accountManager.getOrCreateAccount("Mehrwertsteuer",
			Account.Type.UNKNOWN);
		assertEquals(new BigDecimal("-300.00"), mwstAccount.getBalance());
	}

	public void testReadCategory() {
		List<String> block = new ArrayList<String>();
		block.add("NWertpapierertra");
		block.add("DWertpapiereinkünfte");
		block.add("I");
		Category cat = qifReader.readCategory(block);

		assertEquals("Wertpapierertra", cat.getName());
		assertEquals(Category.Type.INCOME, cat.getType());
	}

	public void testReadComplexCategory() {
		List<String> block = new ArrayList<String>();
		block.add("NFahrzeuge:Benzin");
		block.add("DBenzin, Kraftstoffe");
		block.add("T");
		block.add("R3");
		block.add("E");
		Category cat = qifReader.readCategory(block);

		assertEquals("Benzin", cat.getName());
		assertEquals("Fahrzeuge", cat.getParent().getName());
		assertEquals(Category.Type.EXPENSE, cat.getType());
		assertEquals(Category.Type.EXPENSE, cat.getParent().getType());
	}

	public void testResolveComplementTransferTransactions() {
		Transfer t1 = new Transfer(bankAccount, investmentAccount, new BigDecimal("10.00"),
			new Day());
		bankAccount.addTransaction(t1.getTransferFrom());
		investmentAccount.addTransaction(t1.getTransferTo());
		Transfer t2 = new Transfer(investmentAccount, bankAccount, new BigDecimal("-10.00"),
			new Day());
		investmentAccount.addTransaction(t2.getTransferFrom());
		bankAccount.addTransaction(t2.getTransferTo());
		qifReader.resolveComplementTransferTransactions();

		assertEquals(1, bankAccount.getTransactions().size());
		assertEquals(1, investmentAccount.getTransactions().size());
		assertEquals(new BigDecimal("-10.00"), bankAccount.getBalance());
		assertEquals(new BigDecimal("10.00"), investmentAccount.getBalance());
	}

	public void testResolveComplementSingleSplitTransferTransactions() {
		Transfer t1 = new Transfer(bankAccount, investmentAccount, new BigDecimal("10.00"),
			new Day());
		Transaction s1 = new Transaction(t1.getTransferFrom().getDay(),
			t1.getTransferFrom().getAmount());
		s1.addSplit(t1.getTransferFrom());
		bankAccount.addTransaction(s1);
		investmentAccount.addTransaction(t1.getTransferTo());
		Transfer t2 = new Transfer(investmentAccount, bankAccount, new BigDecimal("-10.00"),
			new Day());
		investmentAccount.addTransaction(t2.getTransferFrom());
		bankAccount.addTransaction(t2.getTransferTo());
		qifReader.resolveComplementTransferTransactions();

		assertEquals(1, bankAccount.getTransactions().size());
		assertEquals(1, investmentAccount.getTransactions().size());
		assertEquals(new BigDecimal("-10.00"), bankAccount.getBalance());
		assertEquals(new BigDecimal("10.00"), investmentAccount.getBalance());
	}

	public void testResolveComplementDubleSplitTransferTransactions() {
		Transfer t1 = new Transfer(bankAccount, investmentAccount, new BigDecimal("10.00"),
			new Day());
		Transaction s1 = new Transaction(t1.getTransferFrom().getDay(),
			t1.getTransferFrom().getAmount());
		s1.addSplit(t1.getTransferFrom());
		bankAccount.addTransaction(s1);
		investmentAccount.addTransaction(t1.getTransferTo());
		Transfer t2 = new Transfer(investmentAccount, bankAccount, new BigDecimal("-10.00"),
			new Day());
		Transaction s2 = new Transaction(t2.getTransferFrom().getDay(),
			t2.getTransferFrom().getAmount());
		s2.addSplit(t2.getTransferFrom());
		investmentAccount.addTransaction(s2);
		bankAccount.addTransaction(t2.getTransferTo());
		qifReader.resolveComplementTransferTransactions();

		assertEquals(1, bankAccount.getTransactions().size());
		assertEquals(1, investmentAccount.getTransactions().size());
		assertEquals(new BigDecimal("-10.00"), bankAccount.getBalance());
		assertEquals(new BigDecimal("10.00"), investmentAccount.getBalance());
	}
}
