package de.tomsplayground.peanuts.client.dividend;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.handlers.HandlerUtil;

import com.google.common.collect.ImmutableSet;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Category;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.base.InventoryEntry;
import de.tomsplayground.peanuts.domain.dividend.Dividend;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction.Type;

public class CreateDividendTransaction extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<Dividend> dividends = new ArrayList<>();
		for (Object o : HandlerUtil.getCurrentStructuredSelection(event).toList()) {
			dividends.add((Dividend)o);
		}
		for (Dividend dividend : dividends) {
			if (dividend.getNettoAmountInDefaultCurrency() == null || dividend.getNettoAmountInDefaultCurrency().signum() == 0) {
				showErrorMessage(event, "No netto amount set.");
				continue;
			}

			AccountManager accountManager = Activator.getDefault().getAccountManager();
			List<Account> invAccounts = accountManager.getAccounts().stream()
				.filter(acc -> acc.getType() == Account.Type.INVESTMENT)
				.collect(Collectors.toList());
			for (Account invAccount : invAccounts) {
				InventoryEntry entry = getInventoryEntry(invAccount, dividend);
				if (entry != null && entry.getQuantity().signum() == 1) {
					invAccount.addTransaction(createTransaction(dividend, accountManager));
					showOkayMessage(event, invAccount);
					break;
				}
			}
		}
		return null;
	}

	private InvestmentTransaction createTransaction(Dividend dividend, AccountManager accountManager) {
		InvestmentTransaction transaction = new InvestmentTransaction(dividend.getPayDate(), dividend.getSecurity(),
			dividend.getNettoAmountInDefaultCurrency(), BigDecimal.ONE, BigDecimal.ZERO, Type.INCOME);
		transaction.setCategory(getDividendCategory(accountManager));
		return transaction;
	}

	private InventoryEntry getInventoryEntry(Account invAccount, Dividend dividend) {
		Inventory inventory = new Inventory(invAccount, null, null, Activator.getDefault().getAccountManager());
		inventory.setDate(dividend.getPayDate());
		InventoryEntry entry = inventory.getEntry(dividend.getSecurity());
		inventory.dispose();
		return entry;
	}

	private void showErrorMessage(ExecutionEvent event, String mesg) {
		MessageBox dialog = new MessageBox(HandlerUtil.getActiveShell(event), SWT.ICON_ERROR | SWT.OK);
		dialog.setText("Add Transaction");
		dialog.setMessage(mesg);
		dialog.open();
	}

	private void showOkayMessage(ExecutionEvent event, Account invAccount) {
		MessageBox dialog = new MessageBox(HandlerUtil.getActiveShell(event), SWT.ICON_INFORMATION | SWT.OK);
		dialog.setText("Add Transaction");
		dialog.setMessage("New transaction added to '" + invAccount.getName() + "'");
		dialog.open();
	}

	private Category getDividendCategory(AccountManager accountManager) {
		ImmutableSet<Category> categories = accountManager.getCategories(Category.Type.INCOME);
		for (Category category : categories) {
			if (category.getName().equals("Dividende")) {
				return category;
			}
		}
		return null;
	}

}
