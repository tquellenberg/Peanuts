package de.tomsplayground.peanuts.client.views;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ViewPart;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.editors.account.AccountEditor;
import de.tomsplayground.peanuts.client.editors.account.AccountEditorInput;
import de.tomsplayground.peanuts.client.util.UniqueAsyncExecution;
import de.tomsplayground.peanuts.client.widgets.DateComposite;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.domain.reporting.investment.AnalyzerFactory;
import de.tomsplayground.peanuts.util.PeanutsUtil;
import de.tomsplayground.util.Day;

public class AccountListView extends ViewPart {

	public static final String ID = "de.tomsplayground.peanuts.client.accountListView";

	private TableViewer accountListViewer;

	private static final FilterActiveAccounts FILTER_ACTIVE_ACCOUNTS = new FilterActiveAccounts();

	private static final class FilterActiveAccounts extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			return ((Account)element).isActive();
		}
	}

	private class AccountListLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider {

		private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
		private final Color red;
		private final Color l1;
		private final Color l2;

		public AccountListLabelProvider(Color red, Color l1, Color l2) {
			this.red = red;
			this.l1 = l1;
			this.l2 = l2;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			Account account = (Account) element;
			currencyFormat.setCurrency(account.getCurrency());
			switch (columnIndex) {
				case 0:
					return account.getName();
				case 1:
					return currencyFormat.format(account.getBalance(date));
				case 2:
					if (account.getType() == Account.Type.INVESTMENT || account.getType() == Account.Type.COMMODITY) {
						return currencyFormat.format(inventories.get(account).getMarketValue());
					}
					break;
				default:
					break;
			}
			return "";
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			return (accounts.indexOf(element) % 2 == 0) ? l1 : l2;
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			Account account = (Account) element;
			if (columnIndex == 1 && account.getBalance().signum() == -1) {
				return red;
			}
			return null;
		}
	}

	private final int colWidth[] = new int[3];

	private Label saldo;

	private Day date = new Day();

	private final PropertyChangeListener propertyChangeListener = new UniqueAsyncExecution() {

		@Override
		public void doit(final PropertyChangeEvent evt, Display display) {
			if (! accountListViewer.getControl().isDisposed()) {
				if (evt.getSource() instanceof Account) {
					Account a = (Account) evt.getSource();
					accountListViewer.update(a, null);
					updateSaldo();
				} else if (evt.getSource() instanceof Inventory) {
					for (Entry<Account, Inventory> entry : inventories.entrySet()) {
						if (entry.getValue() == evt.getSource()) {
							accountListViewer.update(entry.getKey(), null);
						}
					}
					updateSaldo();
				}
			}
		}

		@Override
		public Display getDisplay() {
			return getViewSite().getShell().getDisplay();
		}
	};

	private ImmutableList<Account> accounts;
	private final Map<Account, Inventory> inventories = new HashMap<Account, Inventory>();

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		if (memento != null) {
			for (int i = 0; i < colWidth.length; i++ ) {
				Integer width = memento.getInteger("col" + i);
				if (width != null) {
					colWidth[i] = width.intValue();
				}
			}
		}
	}

	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		TableColumn[] columns = accountListViewer.getTable().getColumns();
		for (int i = 0; i < columns.length; i++ ) {
			TableColumn tableColumn = columns[i];
			memento.putInteger("col" + i, tableColumn.getWidth());
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite top = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		top.setLayout(layout);
		// top banner
		Composite banner = new Composite(top, SWT.NONE);
		banner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		layout = new GridLayout();
		layout.marginHeight = 5;
		layout.marginWidth = 10;
		layout.numColumns = 2;
		banner.setLayout(layout);
		Font boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);

		// Date
		Label l = new Label(banner, SWT.WRAP);
		l.setText("Date:");
		l.setFont(boldFont);
		final DateComposite dateChooser = new DateComposite(banner, SWT.NONE);

		// setup bold font
		l = new Label(banner, SWT.WRAP);
		l.setText("Saldo:");
		l.setFont(boldFont);
		saldo = new Label(banner, SWT.NONE);

		accountListViewer = new TableViewer(top, SWT.MULTI | SWT.FULL_SELECTION);
		Table table = accountListViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		TableColumn col = new TableColumn(table, SWT.LEFT);
		col.setText("Name");
		col.setWidth((colWidth[0] > 0) ? colWidth[0] : 300);
		col.setResizable(true);

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Saldo");
		col.setWidth((colWidth[1] > 0) ? colWidth[1] : 100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Market value");
		col.setWidth((colWidth[2] > 0) ? colWidth[2] : 100);
		col.setResizable(true);

		accountListViewer.setContentProvider(new ArrayContentProvider());
		ColorRegistry colorProvider = Activator.getDefault().getColorProvider();
		Color red = colorProvider.get(Activator.RED);
		accountListViewer.setLabelProvider(new AccountListLabelProvider(red,
			colorProvider.get(Activator.LIST_EVEN), colorProvider.get(Activator.LIST_ODD)));
		accounts = Activator.getDefault().getAccountManager().getAccounts();
		for (Account account : accounts) {
			account.addPropertyChangeListener(propertyChangeListener);
			Inventory inventory = new Inventory(account, PriceProviderFactory.getInstance(), date, new AnalyzerFactory());
			inventory.addPropertyChangeListener(propertyChangeListener);
			inventories.put(account, inventory);
		}
		accountListViewer.setInput(accounts);
		accountListViewer.addFilter(FILTER_ACTIVE_ACCOUNTS);
		accountListViewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getViewer().getSelection();
				Account account = (Account) selection.getFirstElement();
				IEditorInput input = new AccountEditorInput(account);
				try {
					getSite().getWorkbenchWindow().getActivePage().openEditor(input,
						AccountEditor.ID);
				} catch (PartInitException e) {
					e.printStackTrace();
				}
			}
		});
		dateChooser.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				date = dateChooser.getDay();
				for (Inventory inv : inventories.values()) {
					inv.setDate(date);
				}
				accountListViewer.refresh();
				updateSaldo();
			}
		});


		IActionBars actionBars = getViewSite().getActionBars();
		actionBars.setGlobalActionHandler(ActionFactory.REFRESH.getId(), new Action("Refresh") {
			@Override
			public void run() {
				accountListViewer.refresh();
				updateSaldo();
			}
		});
		updateSaldo();
	}

	@Override
	public void dispose() {
		super.dispose();
		for (Entry<Account, Inventory> entry : inventories.entrySet()) {
			entry.getKey().removePropertyChangeListener(propertyChangeListener);
			entry.getValue().removePropertyChangeListener(propertyChangeListener);
			entry.getValue().dispose();
		}
	}

	private void updateSaldo() {
		final Map<Currency, BigDecimal> saldoMap = new HashMap<Currency, BigDecimal>();
		for (Account a : accounts) {
			BigDecimal s = BigDecimal.ZERO;
			if (saldoMap.containsKey(a.getCurrency())) {
				s = saldoMap.get(a.getCurrency());
			}
			s = s.add(a.getBalance(date));
			if (a.getType() == Account.Type.INVESTMENT || a.getType() == Account.Type.COMMODITY) {
				s = s.add(inventories.get(a).getMarketValue());
			}
			saldoMap.put(a.getCurrency(), s);
		}
		ArrayList<Currency> currencies = new ArrayList<Currency>(saldoMap.keySet());
		Collections.sort(currencies, new Comparator<Currency>() {
			@Override
			public int compare(Currency o1, Currency o2) {
				BigDecimal s1 = saldoMap.get(o1);
				BigDecimal s2 = saldoMap.get(o2);
				return s2.compareTo(s1);
			}});
		String saldoStr = "";
		for (Currency currency : currencies) {
			if (saldoStr.length() > 0) {
				saldoStr += "; ";
			}
			saldoStr = saldoStr + PeanutsUtil.formatCurrency(saldoMap.get(currency), currency);
		}
		saldo.setText(saldoStr);
		saldo.getParent().layout();
	}

	@Override
	public void setFocus() {
		accountListViewer.getTable().setFocus();
	}

}
