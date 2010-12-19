package de.tomsplayground.peanuts.client.quicken;

import java.util.Arrays;
import java.util.Currency;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.Account;

public class QifAccountPage extends WizardPage {

	private TableViewer viewer;

	private static class AccountLabelProvider extends LabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			Account account = (Account) element;
			switch (columnIndex) {
				case 0:
					return account.getName();
				case 1:
					return account.getCurrency().toString();
			}
			return "";
		}

	}

	protected QifAccountPage(String pageName) {
		super(pageName);
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());

		viewer = new TableViewer(composite, SWT.H_SCROLL);
		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));

		TableColumn col1 = new TableColumn(table, SWT.LEFT);
		col1.setText("Name");
		col1.setWidth(300);
		col1.setResizable(true);

		TableColumn col2 = new TableColumn(table, SWT.LEFT);
		col2.setText("Währung");
		col2.setWidth(100);
		col2.setResizable(true);

		viewer.setLabelProvider(new AccountLabelProvider());
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setColumnProperties(new String[] { "1", "2" });
		viewer.setCellEditors(new CellEditor[] { null,
			new ComboBoxCellEditor(table, Activator.getDefault().getCurrencies()) });
		viewer.setCellModifier(new ICellModifier() {

			@Override
			public boolean canModify(Object element, String property) {
				return property.equals("2");
			}

			@Override
			public Object getValue(Object element, String property) {
				if (property.equals("2")) {
					Account account = (Account) element;
					String symbol = account.getCurrency().getCurrencyCode();
					int i = Arrays.asList(Activator.getDefault().getCurrencies()).indexOf(symbol);
					return Integer.valueOf(i);
				}
				return null;
			}

			@Override
			public void modify(Object element, String property, Object value) {
				if (property.equals("2")) {
					TableItem tableItem = (TableItem) element;
					Account account = (Account) ((tableItem).getData());
					Integer i = (Integer) value;
					String symbol = Activator.getDefault().getCurrencies()[i.intValue()];
					account.setCurrency(Currency.getInstance(symbol));
					tableItem.setText(1, symbol);
				}
			}
		});

		setControl(composite);
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			setPageComplete(true);
		}
	}

	public void setAccounts(List<Account> accounts) {
		viewer.setInput(accounts.toArray(new Account[0]));
	}

}
