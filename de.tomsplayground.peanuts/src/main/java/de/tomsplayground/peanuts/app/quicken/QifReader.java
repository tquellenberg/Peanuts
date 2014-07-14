package de.tomsplayground.peanuts.app.quicken;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Category;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.BankTransaction;
import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.peanuts.domain.process.ITransferLocation;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.domain.process.Transaction;
import de.tomsplayground.peanuts.domain.process.Transfer;
import de.tomsplayground.peanuts.domain.process.TransferTransaction;
import de.tomsplayground.util.Day;

public class QifReader {

	final static Logger log = Logger.getLogger(QifReader.class);

	AccountManager accountManager;

	final static Map<String, Account.Type> accountTypeMap = new ConcurrentHashMap<String, Account.Type>();

	static {
		accountTypeMap.put("Bank", Account.Type.BANK);
		accountTypeMap.put("Oth A", Account.Type.ASSET);
		accountTypeMap.put("Oth D", Account.Type.ASSET);
		accountTypeMap.put("Oth E", Account.Type.ASSET);
		accountTypeMap.put("Oth F", Account.Type.ASSET);
		accountTypeMap.put("Oth L", Account.Type.LIABILITY);
		accountTypeMap.put("Oth P", Account.Type.ASSET);
		accountTypeMap.put("Invst", Account.Type.INVESTMENT);
		accountTypeMap.put("Port", Account.Type.INVESTMENT);
		accountTypeMap.put("Mutual", Account.Type.INVESTMENT);
		accountTypeMap.put("CCard", Account.Type.BANK);
		accountTypeMap.put("Cash", Account.Type.BANK);
	}

	public List<String> nextBlock(LineNumberReader in) throws IOException {
		List<String> block = new ArrayList<String>();
		String line = in.readLine();
		while (line != null && !line.equals("^")) {
			block.add(line);
			line = in.readLine();
		}
		return block;
	}

	private static final class State {
		boolean accountList;

		String blockType;

		Account currentAccount;
	}

	public void interpretBlock(List<String> block, State state) {
		String l = block.get(0).trim();
		while (l.startsWith("!")) {
			block.remove(0);
			state.blockType = null;
			if (l.equals("!Option:AutoSwitch")) {
				// Ignore
			} else if (l.equals("!Clear:AutoSwitch")) {
				state.accountList = false;
			} else if (l.equals("!Account")) {
				state.accountList = true;

			} else if (l.equals("!Type:Bank")) {
				state.blockType = "Bank";
			} else if (l.equals("!Type:Oth A")) {
				state.blockType = "Bank";
			} else if (l.equals("!Type:Oth D")) {
				state.blockType = "Bank";
			} else if (l.equals("!Type:Oth E")) {
				state.blockType = "Bank";
			} else if (l.equals("!Type:Oth F")) {
				state.blockType = "Bank";
			} else if (l.equals("!Type:Oth L")) {
				state.blockType = "Bank";
			} else if (l.equals("!Type:Oth P")) {
				state.blockType = "Bank";
			} else if (l.equals("!Type:CCard")) {
				state.blockType = "Bank";
			} else if (l.equals("!Type:Cash")) {
				state.blockType = "Bank";
			} else if (l.equals("!Type:Invst")) {
				state.blockType = "Invst";

			} else if (l.equals("!Type:Cat")) {
				state.accountList = false;
				state.blockType = "CatList";
			} else if (l.equals("!Type:Memorized")) {
				state.accountList = false;
			} else if (l.equals("!Type:Class")) {
				state.accountList = false;
			} else if (l.equals("!Type:Security")) {
				state.accountList = false;
			} else if (l.equals("!Type:Prices")) {
				state.accountList = false;
			} else if (l.equals("!Type:Budget")) {
				state.accountList = false;
			} else if (l.equals("!Type:Invitem")) {
				state.accountList = false;
			} else if (l.equals("!Type:Template")) {
				state.accountList = false;
			} else {
				log.error("=======Unknown header:" + l);
			}
			l = block.get(0);
		}
		if (state.accountList) {
			if (state.blockType == null) {
				log.debug("Account:" + l);
				state.currentAccount = readAccount(block);
			} else if (state.blockType.equals("Bank")) {
				readTransaction(block, state.currentAccount);
			} else if (state.blockType.equals("Invst")) {
				if (isInvestmentTransaction(block)) {
					readInvestmentTransaction(block, state.currentAccount);
				} else {
					readTransaction(block, state.currentAccount);
				}
			}
		} else {
			if (state.blockType.equals("CatList")) {
				readCategory(block);
			}
		}
	}

	private boolean isInvestmentTransaction(List<String> block) {
		for (String s : block) {
			if (s.startsWith("Y")) {
				return true;
			}
		}
		return false;
	}

	public Account readAccount(List<String> block) {
		String description = "";
		String name = "";
		Account.Type type = Account.Type.UNKNOWN;
		while ( !block.isEmpty()) {
			String l = block.get(0);
			char c = l.charAt(0);
			l = l.substring(1);
			switch (c) {
				case 'N':
					name = l;
					break;
				case 'D':
					description = l;
					break;
				case 'T':
					type = accountTypeMap.get(l);
					if (type == null) {
						log.error("Unknown Account-Type:" + l);
					}
					break;
				default:
					log.error("Unknown Letter in account:" + c);
					break;
			}
			block.remove(0);
		}
		Account account = accountManager.getOrCreateAccount(name, type);
		account.setDescription(description);
		return account;
	}

	public void read(Reader reader) throws IOException {
		LineNumberReader in = new LineNumberReader(reader);
		List<String> block = nextBlock(in);
		State state = new State();

		try {
			while (block.size() > 0) {
				log.debug("Block:" + block.get(0));
				interpretBlock(block, state);
				block = nextBlock(in);
			}
			resolveComplementTransferTransactions();
		} catch (RuntimeException e) {
			log.error("Error near line " + in.getLineNumber(), e);
		}
	}

	public List<Transaction> readSplits(List<String> block, ITransferLocation account, Day date) {
		List<Transaction> splits = new ArrayList<Transaction>();
		List<String> splitBlock = new ArrayList<String>();
		while ( !block.isEmpty()) {
			String line = block.remove(0);
			if (line.startsWith("S") && splitBlock.size() > 0) {
				splits.add(readSplit(splitBlock, account, date));
				splitBlock.clear();
			}
			splitBlock.add(line);
		}
		if (splitBlock.size() > 0) {
			splits.add(readSplit(splitBlock, account, date));
		}
		return splits;
	}

	public Transaction readSplit(List<String> block, ITransferLocation account, Day date) {
		String memo = "";
		BigDecimal amount = BigDecimal.ZERO;
		Category cat = null;
		Account transferAccount = null;
		while ( !block.isEmpty()) {
			String l = block.get(0);
			char c = l.charAt(0);
			l = l.substring(1);
			switch (c) {
				case 'S':
					if (l.startsWith("[") && l.indexOf("]") != -1) {
						l = l.substring(1, l.indexOf("]"));
						transferAccount = accountManager.getOrCreateAccount(l, Account.Type.UNKNOWN);
					} else {
						cat = interpretCategory(l, Category.Type.UNKNOWN);
					}
					break;
				case '$':
					amount = readAmount(l);
					break;
				case 'E':
					memo = l;
					break;
				case '%':
					throw new RuntimeException();
			}
			block.remove(0);
		}
		Transaction trans;
		if (transferAccount != null && transferAccount != account) {
			Transfer transfer = new Transfer(account, transferAccount, amount.negate(), date);
			transfer.setMemo(memo);
			trans = transfer.getTransferFrom();
			transferAccount.addTransaction(transfer.getTransferTo());
		} else {
			trans = new Transaction(new Day(), amount, cat, memo);
		}
		return trans;
	}

	public Transaction readInvestmentTransaction(List<String> block, Account account) {
		Day date = null;
		BigDecimal price = BigDecimal.ZERO;
		BigDecimal quantity = BigDecimal.ZERO;
		BigDecimal amount = BigDecimal.ZERO;
		Category category = null;
		BigDecimal transferAmmount = BigDecimal.ZERO;
		BigDecimal commission = BigDecimal.ZERO;
		Security security = null;
		String memo = "";
		Account transferAccount = null;
		InvestmentTransaction.Type type = null;
		while ( !block.isEmpty()) {
			String l = block.get(0);
			char c = l.charAt(0);
			l = l.substring(1);
			switch (c) {
				case 'D':
					date = readDay(l);
					break;
				case 'I':
					price = readQuantity(l);
					break;
				case 'N':
					if (l.startsWith("Kauf") || l.equals("ReinvDiv") || l.equals("AktSplit")) {
						type = InvestmentTransaction.Type.BUY;
					} else if (l.startsWith("Verkauf") || l.equals("AktAb")) {
						type = InvestmentTransaction.Type.SELL;
					} else if (l.equals("VersAus") || l.equals("ZinsAus")) {
						type = InvestmentTransaction.Type.EXPENSE;
						category = accountManager.getOrCreateCategory(l);
					} else if (l.equals("Div") || l.equals("ZinsEin")) {
						type = InvestmentTransaction.Type.INCOME;
						category = accountManager.getOrCreateCategory(l);
					} else {
						log.error("Unknown investment transaction type " + l);
					}
					break;
				case 'Q':
					quantity = readQuantity(l);
					break;
				case 'O':
					commission = readAmount(l);
					break;
				case 'M':
					memo = l;
					break;
				case 'Y':
					security = accountManager.getOrCreateSecurity(l);
					break;
				case 'L':
					if (l.startsWith("[") && l.endsWith("]")) {
						l = l.substring(1, l.length() - 1);
						transferAccount = accountManager.getOrCreateAccount(l, Account.Type.UNKNOWN);
					} else {
						log.error("Not supported value in investment transaction" + l);
					}
					break;
				case '$':
					transferAmmount = readAmount(l);
					break;
				case 'C':
					// Checked
					break;
				case 'U':
				case 'T':
					amount = readAmount(l);
					break;
				default:
					log.error("Unknown Letter in investment transaction:" + c);
					break;
			}
			block.remove(0);
		}
		Transaction trans;
		if (type == InvestmentTransaction.Type.INCOME) {
			trans = new InvestmentTransaction(date, security, amount, BigDecimal.ONE, BigDecimal.ZERO, type);
		} else if (type == InvestmentTransaction.Type.EXPENSE) {
			trans = new InvestmentTransaction(date, security, amount.negate(), BigDecimal.ONE, BigDecimal.ZERO, type);
		} else {
			trans = new InvestmentTransaction(date, security, price, quantity, commission, type);
		}
		trans.setMemo(memo);
		trans.setCategory(category);
		if (transferAccount != null && type == InvestmentTransaction.Type.BUY) {
			Transaction t = new Transaction(date, BigDecimal.ZERO);
			t.setMemo(memo);
			Transfer transfer = new Transfer(account, transferAccount, transferAmmount.negate(), date);
			transferAccount.addTransaction(transfer.getTransferTo());
			t.addSplit(transfer.getTransferFrom());
			t.addSplit(trans);
			account.addTransaction(t);
			trans = t;
		} else {
			account.addTransaction(trans);
		}
		return trans;
	}

	public Transaction readTransaction(List<String> block, Account account) {
		Day date = null;
		BigDecimal amount = BigDecimal.ZERO;
		String payee = "";
		String memo = "";
		Category category = null;
		Category category2 = null;
		List<Transaction> splits = null;
		Account transferAccount = null;
		boolean negate = false;
		while ( !block.isEmpty()) {
			String l = block.get(0);
			char c = l.charAt(0);
			l = l.substring(1);
			switch (c) {
				case 'D':
					date = readDay(l);
					break;
				case 'U':
					amount = readAmount(l);
					break;
				case 'T':
				case 'C':
					break;
				case 'P':
					payee = l;
					break;
				case 'M':
					memo = l;
					break;
				case 'L':
					if (l.startsWith("[") && l.endsWith("]")) {
						l = l.substring(1, l.length() - 1);
						transferAccount = accountManager.getOrCreateAccount(l, Account.Type.UNKNOWN);
					} else {
						category = interpretCategory(l, Category.Type.UNKNOWN);
					}
					break;
				case 'S':
					splits = readSplits(block, account, date);
					block.add(0, "Dummy");
					break;
				case 'N':
					category2 = accountManager.getOrCreateCategory(l);
					if (l.equals("XAus") || l.equals("VersAus") || l.equals("ZinsAus")) {
						negate = true;
					} else if (l.equals("XEin") || l.equals("VersEin") || l.equals("ZinsEin") ||
						l.equals("Umrech")) {
						// Okay
					} else {
						log.error("Unknow Type:" + l);
					}
					break;
				default:
					log.error("Unknown Letter in transaction:" + c);
					break;
			}
			block.remove(0);
		}
		if (negate) {
			amount = amount.negate();
		}
		Transaction trans = null;
		if (transferAccount != null && transferAccount != account && splits == null) {
			Transfer transfer = new Transfer(account, transferAccount, amount.negate(), date);
			transfer.setLabel(payee);
			transfer.setMemo(memo);
			transferAccount.addTransaction(transfer.getTransferTo());
			trans = transfer.getTransferFrom();
			account.addTransaction(trans);
		} else {
			if (category == null) {
				category = category2;
			}
			trans = new BankTransaction(date, amount, payee);
			trans.setCategory(category);
			trans.setMemo(memo);
			if (splits != null) {
				for (Transaction t : splits) {
					trans.addSplit(t);
				}
			}
			account.addTransaction(trans);
		}
		return trans;
	}

	private Category interpretCategory(String c, Category.Type type) {
		String[] cs = StringUtils.split(c, ':');
		Category cat = new Category(cs[0], type);
		Category result = cat;
		if (cs.length > 1) {
			result = new Category(cs[1], type);
			cat.addChildCategory(result);
		}
		accountManager.addCategory(cat);
		return result;
	}

	private Day readDay(String l) {
		Day date = null;
		try {
			date = Day.fromDate((new SimpleDateFormat("M.d.yy")).parse(l));
		} catch (ParseException e) {
			log.error("", e);
		}
		return date;
	}

	public BigDecimal readQuantity(String l) {
		l = StringUtils.remove(l, '.');
		l = l.replace(',', '.');
		return new BigDecimal(l);
	}

	public BigDecimal readAmount(String l) {
		if (l.indexOf('.') != -1) {
			l = StringUtils.remove(l, ',');
		} else {
			l = l.replace(',', '.');
		}
		return new BigDecimal(l);
	}

	public void setAccountManager(AccountManager accountManager) {
		this.accountManager = accountManager;
	}

	public Category readCategory(List<String> block) {
		String name = "";
		Category.Type type = Category.Type.UNKNOWN;
		while ( !block.isEmpty()) {
			String l = block.get(0);
			char c = l.charAt(0);
			l = l.substring(1);
			switch (c) {
				case 'N':
					name = l;
					break;
				case 'I':
					type = Category.Type.INCOME;
					break;
				case 'E':
					type = Category.Type.EXPENSE;
					break;
				default:
					log.error("Unknown Letter in category:" + c);
					break;
			}
			block.remove(0);
		}
		return interpretCategory(name, type);
	}

	public void resolveComplementTransferTransactions() {
		for (Account account : accountManager.getAccounts()) {
			for (ITransaction trans : account.getTransactions()) {
				if (trans instanceof TransferTransaction) {
					TransferTransaction transfer = (TransferTransaction) trans;
					if ( !transfer.isSource()) {
						checkComplementTransfer(account, transfer);
					}
				}
			}
		}
	}

	private void checkComplementTransfer(Account sourceAccount, TransferTransaction transfer) {
		Account targetAccount = (Account) transfer.getTarget();

		List<ITransaction> targetTransactions = targetAccount.getTransactionsByDate(transfer.getDay());
		for (ITransaction targetTransaction : targetTransactions) {
			if (targetTransaction instanceof TransferTransaction) {
				TransferTransaction targetTransfer = (TransferTransaction) targetTransaction;
				if ( !targetTransfer.isSource() && targetTransfer.getTarget() == sourceAccount &&
					targetTransfer.getAmount().compareTo(transfer.getComplement().getAmount()) == 0) {
					transfer.getComplement().setComplement(targetTransfer.getComplement());
					targetTransfer.getComplement().setComplement(transfer.getComplement());
					targetAccount.removeTransaction(targetTransfer);
					sourceAccount.removeTransaction(transfer);
					break;
				}
			}
		}
	}

}
