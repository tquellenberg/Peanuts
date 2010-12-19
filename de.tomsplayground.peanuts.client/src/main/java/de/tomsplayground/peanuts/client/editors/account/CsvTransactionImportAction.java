package de.tomsplayground.peanuts.client.editors.account;

import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;

import de.tomsplayground.peanuts.app.csv.StarMoneyCsvReader;
import de.tomsplayground.peanuts.client.ICommandIds;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.process.BankTransaction;

public class CsvTransactionImportAction extends Action {

	private IEditorPart editorPart;

	public CsvTransactionImportAction() {
		super();
		setId(ICommandIds.CMD_CSV_TRANSACTION_IMPORT);
		setActionDefinitionId(ICommandIds.CMD_CSV_TRANSACTION_IMPORT);
	}
	
	@Override
	public void run() {
		Shell shell = editorPart.getSite().getShell();
		FileDialog openDialog = new FileDialog(shell, SWT.OPEN);
		openDialog.setFilterExtensions(new String[] { "CSV" });
		String filename = openDialog.open();
		if (filename != null) {
			FileReader reader = null;
			try {
				reader = new FileReader(filename);
				StarMoneyCsvReader csvReader = new StarMoneyCsvReader(reader);
				csvReader.read();
				List<BankTransaction> importTransactions = csvReader.getTransactions();
				AccountEditorInput input = (AccountEditorInput)editorPart.getEditorInput();
				Account account = input.getAccount();
				for (BankTransaction importTransaction : importTransactions) {
					account.addTransaction(importTransaction);
				}
			} catch (IOException e) {
				MessageDialog.openError(shell, "Error", e.getMessage());
			} catch (ParseException e) {
				MessageDialog.openError(shell, "Error", e.getMessage());
			} finally {
				IOUtils.closeQuietly(reader);				
			}
		}
	}

	public void setEditorPart(IEditorPart editorPart) {
		this.editorPart = editorPart;
	}
	
}
