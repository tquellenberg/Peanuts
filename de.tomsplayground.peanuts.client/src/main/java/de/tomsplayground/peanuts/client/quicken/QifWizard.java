package de.tomsplayground.peanuts.client.quicken;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;

import de.tomsplayground.peanuts.app.quicken.QifReader;
import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.AccountManager;

public class QifWizard extends Wizard implements IPageChangingListener {

	private File file;
	private QifAccountPage accountPage;
	private EncodingPage encodingPage;
	private AccountManager dummyAccountManager;

	public QifWizard(File file) {
		this.file = file;
	}

	@Override
	public void addPages() {
		encodingPage = new EncodingPage("Encoding");
		addPage(encodingPage);
		accountPage = new QifAccountPage("Accounts");
		addPage(accountPage);
	}

	private void loadFile(String charset, AccountManager accountManager) throws IOException {
		QifReader qifReader = new QifReader();
		qifReader.setAccountManager(accountManager);
		InputStreamReader qifFile = new InputStreamReader(new FileInputStream(file), charset);
		qifReader.read(qifFile);
		qifFile.close();
		accountPage.setAccounts(accountManager.getAccounts());
	}

	@Override
	public void setContainer(IWizardContainer wizardContainer) {
		super.setContainer(wizardContainer);
		if (wizardContainer != null) {
			((WizardDialog) wizardContainer).addPageChangingListener(this);
		}
	}

	@Override
	public boolean performFinish() {
		AccountManager accountManager = Activator.getDefault().getAccountManager();
		accountManager.reset();

		List<Account> accounts = dummyAccountManager.getAccounts();
		for (Account account : accounts) {
			Account a = accountManager.getOrCreateAccount(account.getName(), account.getType());
			a.setCurrency(account.getCurrency());
		}
		try {
			loadFile(encodingPage.getSelectedCharset(), accountManager);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return true;
	}

	@Override
	public void handlePageChanging(PageChangingEvent event) {
		if (event.getTargetPage() == accountPage) {
			String selectedCharset = encodingPage.getSelectedCharset();
			try {
				dummyAccountManager = new AccountManager();
				loadFile(selectedCharset, dummyAccountManager);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
