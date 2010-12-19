package de.tomsplayground.peanuts.client.wizards.report;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.Account;

public class AccountsPage extends WizardPage {

	private TableViewer listViewer;

	protected AccountsPage(String pageName) {
		super(pageName);
		setTitle("Select accounts");
		setMessage("Only transactions of the selected accounts will be included in the report.");
		setDescription("Select accounts");
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite top = new Composite(parent, SWT.NONE);
		top.setLayout(new GridLayout(2, false));

		listViewer = new TableViewer(top, SWT.CHECK);
		listViewer.getTable().setLinesVisible(true);
		listViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		listViewer.setLabelProvider(new AccountListLabelProvider());
		listViewer.setContentProvider(new ArrayContentProvider());
		List<Account> accounts = Activator.getDefault().getAccountManager().getAccounts();
		listViewer.setInput(accounts);
		listViewer.getTable().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (event.detail == SWT.CHECK) {
					setPageComplete( !getAccounts().isEmpty());
				}
			}
		});

		Composite buttons = new Composite(top, SWT.NONE);
		buttons.setLayout(new GridLayout(1, false));
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		Button selectAll = new Button(buttons, SWT.PUSH);
		selectAll.setText("Select all");
		selectAll.setLayoutData(new GridData(SWT.FILL, SWT.CANCEL, true, false));
		selectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TableItem[] items = listViewer.getTable().getItems();
				for (TableItem tableItem : items) {
					tableItem.setChecked(true);
					setPageComplete(true);
				}
			}
		});

		Button deselectAll = new Button(buttons, SWT.PUSH);
		deselectAll.setText("Deselect all");
		deselectAll.setLayoutData(new GridData(SWT.FILL, SWT.CANCEL, true, false));
		deselectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TableItem[] items = listViewer.getTable().getItems();
				for (TableItem tableItem : items) {
					tableItem.setChecked(false);
					setPageComplete(false);
				}
			}
		});

		setControl(top);
	}

	public List<Account> getAccounts() {
		List<Account> accounts = new ArrayList<Account>();
		TableItem[] items = listViewer.getTable().getItems();
		for (TableItem tableItem : items) {
			if (tableItem.getChecked()) {
				accounts.add((Account) tableItem.getData());
			}
		}
		return accounts;
	}

}
