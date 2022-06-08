package de.tomsplayground.peanuts.client.savedtransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.process.SavedTransaction;
import de.tomsplayground.peanuts.domain.process.Transaction;
import de.tomsplayground.peanuts.util.Day;

public class SavedTransactionManager {

	private final static Logger log = LoggerFactory.getLogger(SavedTransactionManager.class);

	public static void createFuturTransactions(int daysInFuture) {
		AccountManager accountManager = Activator.getDefault().getAccountManager();
		createFuturTransactions(accountManager, daysInFuture);
	}

	private static void createFuturTransactions(AccountManager accountManager, int daysInFuture) {
		accountManager.getSavedTransactions().stream()
			.filter(st -> ! st.isDeleted())
			.forEach(st -> createFuturTransaction(st, daysInFuture));
	}

	public static void createFuturTransaction(SavedTransaction st, int daysInFuture) {
		if (! st.isAutomaticExecution()) {
			return;
		}
		log.info("Check saved transaction {}", st);
		Account account = getAccountForTransaction(st.getTransaction());
		if (account != null && account.isActive()) {
			Day dayLimit = Day.today().addDays(daysInFuture);
			while (st.getStart().before(dayLimit)) {
				if (st.getStart().after(Day.today())) {
					Transaction newTransaction = (Transaction) st.getTransaction().clone();
					newTransaction.setDay(st.getStart());
					log.info("Add new transaction {} to {}", newTransaction, account.getName());
					account.addTransaction(newTransaction);
				} else {
					log.info("No new transaction added because date is in the past {}", st.getStart());
				}
				st.setStart(st.nextExecution());
			}
		} else {
			log.info("Saved transaction {} not found in any active account", st.getName());
		}
	}

	public static Account getAccountForTransaction(Transaction transaction) {
		for (Account account : Activator.getDefault().getAccountManager().getAccounts()) {
			if (account.getTransactions().contains(transaction)) {
				return account;
			}
		}
		return null;
	}

}

