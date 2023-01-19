package de.tomsplayground.peanuts.client.editors.security;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.text.ParseException;
import java.time.Month;
import java.util.Currency;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.util.UniqueAsyncExecution;
import de.tomsplayground.peanuts.client.widgets.DateCellEditor;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.base.InventoryEntry;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.currenncy.Currencies;
import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;
import de.tomsplayground.peanuts.domain.dividend.Dividend;
import de.tomsplayground.peanuts.domain.fundamental.FundamentalDatas;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.domain.query.SecurityInvestmentQuery;
import de.tomsplayground.peanuts.domain.reporting.investment.AnalyzerFactory;
import de.tomsplayground.peanuts.domain.reporting.transaction.Report;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class DividendEditorPart extends EditorPart {
	
	private static final Range<Integer> TWELVE_MONTH_RANGE = Range.between(0, 365-20);

	private boolean dirty;

	private TableViewer tableViewer;

	private final int colWidth[] = new int[15];

	private List<Dividend> dividends;

	private Report securityReport;

	private Inventory securityInventory;

	private ImmutableList<Currency> currencies;

	private final PropertyChangeListener propertyChangeListener = new UniqueAsyncExecution() {
		@Override
		public void doit(PropertyChangeEvent evt, Display display) {
			InvestmentTransaction invTr = null;
			if (evt.getNewValue() instanceof InvestmentTransaction) {
				invTr = (InvestmentTransaction) evt.getNewValue();
			} else if (evt.getOldValue() instanceof InvestmentTransaction) {
				invTr = (InvestmentTransaction) evt.getOldValue();
			}
			if (invTr != null && invTr.getSecurity().equals(getSecurity())) {
				tableViewer.refresh(true);
			}
		}

		@Override
		public Display getDisplay() {
			return getSite().getShell().getDisplay();
		}
	};

	private class DividendTableLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider {

		@Override
		public String getColumnText(Object element, int columnIndex) {
			Dividend entry = (Dividend) element;
			switch (columnIndex) {
				case 0:
					return PeanutsUtil.formatDate(entry.getPayDate());
				case 1:
					return PeanutsUtil.format(entry.getAmountPerShare(), 4);
				case 2:
					return entry.getCurrency().getSymbol();
				case 3:
					return PeanutsUtil.formatQuantity(getQuantity(entry));
				case 4:
					if (entry.getAmount() != null) {
						return PeanutsUtil.formatCurrency(entry.getAmount(), entry.getCurrency());
					} else {
						return PeanutsUtil.formatCurrency(getQuantity(entry).multiply(entry.getAmountPerShare(), PeanutsUtil.MC), entry.getCurrency());
					}
				case 5:
					BigDecimal amount = entry.getAmountInDefaultCurrency();
					if (amount == null) {
						amount = entry.getAmount();
						if (amount == null) {
							amount = getQuantity(entry).multiply(entry.getAmountPerShare());
						}
						CurrencyConverter converter = Activator.getDefault().getExchangeRates()
							.createCurrencyConverter(entry.getCurrency(), Currencies.getInstance().getDefaultCurrency());
						amount = converter.convert(amount, entry.getPayDate());
					}
					return PeanutsUtil.formatCurrency(amount, Currencies.getInstance().getDefaultCurrency());
				case 6:
					if (entry.getTaxInDefaultCurrency() != null) {
						return PeanutsUtil.formatCurrency(entry.getTaxInDefaultCurrency(), Currencies.getInstance().getDefaultCurrency());
					} else {
						return PeanutsUtil.formatCurrency(BigDecimal.ZERO, Currencies.getInstance().getDefaultCurrency());
					}
				case 7:
					if (entry.getTaxInDefaultCurrency() != null && entry.getAmountInDefaultCurrency() != null) {
						return PeanutsUtil.formatPercent(entry.getTaxInDefaultCurrency().divide(entry.getAmountInDefaultCurrency(), PeanutsUtil.MC));
					} else {
						return PeanutsUtil.formatPercent(BigDecimal.ZERO);
					}
				case 8:
					return PeanutsUtil.formatCurrency(entry.getNettoAmountInDefaultCurrency(), Currencies.getInstance().getDefaultCurrency());
				case 9:
					return PeanutsUtil.formatPercent(getDividendYoc(twelveMonthTrailingDividends(entry)));
				case 10:
					InvestmentTransaction booked = isBooked(entry);
					if (booked != null) {
						return PeanutsUtil.formatDate(booked.getDay());
					}
					return "";
				default:
					return "";
			}
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			Dividend entry = (Dividend) element;
			switch (columnIndex) {
				case 3:
					return entry.getQuantity() == null ? Activator.getDefault().getColorProvider().get(Activator.INACTIVE_ROW) : null;
				case 4:
					return entry.getAmount() == null ? Activator.getDefault().getColorProvider().get(Activator.INACTIVE_ROW) : null;
				case 5:
					return entry.getAmountInDefaultCurrency() == null ? Activator.getDefault().getColorProvider().get(Activator.INACTIVE_ROW) : null;
				case 6:
				case 7:
					return entry.getTaxInDefaultCurrency() == null ? Activator.getDefault().getColorProvider().get(Activator.INACTIVE_ROW) : null;
			}
			return null;
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			Dividend entry = (Dividend) element;
			if (columnIndex == 0 && entry.getPayDate().delta(Day.today()) > 7) {
				return Activator.getDefault().getColorProvider().get(Activator.GRAY_BG);
			}
			if (entry.isIncrease()) {
				return Activator.getDefault().getColorProvider().get(Activator.GREEN_BG);
			}
			return null;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (!(input instanceof SecurityEditorInput)) {
			throw new PartInitException("Invalid Input: Must be SecurityEditorInput");
		}
		setSite(site);
		setInput(input);
		setPartName(input.getName());

		securityReport = new Report("securityReport: " + getSecurity().getName());
		securityReport.addQuery(new SecurityInvestmentQuery(getSecurity()));
		securityReport.addPropertyChangeListener("transactions", propertyChangeListener);
		
		AccountManager accountManager = Activator.getDefault().getAccountManager();
		securityReport.setAccounts(accountManager.getAccounts());
		securityInventory = new Inventory(securityReport, PriceProviderFactory.getInstance(), new AnalyzerFactory(),
				 Activator.getDefault().getAccountManager());

		currencies = Currencies.getInstance().getCurrenciesWithExchangeSecurity(accountManager);
	}
	
	@Override
	public void dispose() {
		securityInventory.dispose();
		securityReport.removePropertyChangeListener("transactions", propertyChangeListener);
	}

	@Override
	public void createPartControl(Composite parent) {
		final Security security = getSecurity();
		dividends = cloneDividends(security.getDividends());

		Composite top = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		top.setLayout(layout);

		tableViewer = new TableViewer(top, SWT.FULL_SELECTION | SWT.MULTI);
		Table table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setFont(Activator.getDefault().getNormalFont());

		int colNumber = 0;
		TableColumn col = new TableColumn(table, SWT.LEFT);
		col.setText("Date");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 80);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Dividend");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 80);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Currency");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 70);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("# of shares");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 70);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Dividend sum");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Dividend sum "+Currencies.getInstance().getDefaultCurrency().getSymbol());
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Tax "+Currencies.getInstance().getDefaultCurrency().getSymbol());
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Tax %");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Netto "+Currencies.getInstance().getDefaultCurrency().getSymbol());
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("YOC");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Booked");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.setResizable(true);
		colNumber++;

		tableViewer.setColumnProperties(new String[] { "payDay", "dividend", "currency", "numberOfShares", "amount",
			"amountInDefaultCurrency", "tax", "netto", "booked"});
		tableViewer.setCellModifier(new ICellModifier() {

			@Override
			public boolean canModify(Object element, String property) {
				return List.of("payDay", "dividend", "currency", "numberOfShares", "amount",
					"amountInDefaultCurrency", "tax").contains(property);
			}

			@Override
			public Object getValue(Object element, String property) {
				Dividend p = (Dividend) element;
				if (property.equals("payDay")) {
					return p.getPayDate();
				} else if (property.equals("dividend")) {
					return PeanutsUtil.format(p.getAmountPerShare(), 4);
				} else if (property.equals("currency")) {
					return getCurrencyPos(p.getCurrency());
				} else if (property.equals("numberOfShares")) {
					return p.getQuantity()!=null?PeanutsUtil.formatQuantity(p.getQuantity()):"";
				} else if (property.equals("amount")) {
					return p.getAmount()!=null?PeanutsUtil.formatCurrency(p.getAmount(), null):"";
				} else if (property.equals("amountInDefaultCurrency")) {
					return p.getAmountInDefaultCurrency()!=null?PeanutsUtil.formatCurrency(p.getAmountInDefaultCurrency(), null):"";
				} else if (property.equals("tax")) {
					return p.getTaxInDefaultCurrency()!=null?PeanutsUtil.formatCurrency(p.getTaxInDefaultCurrency(), null):"";
				}
				return null;
			}

			@Override
			public void modify(Object element, String property, Object value) {
				Dividend p = (Dividend) ((TableItem) element).getData();
				try {
					if (property.equals("payDay")) {
						Day payDay = (Day) value;
						if (! payDay.equals(p.getPayDate())) {
							p.setPayDate(payDay);
							tableViewer.update(p, new String[]{property});
							markDirty();
						}
					} else if (property.equals("dividend")) {
						BigDecimal dividend = PeanutsUtil.parseCurrency((String) value);
						if (! dividend.equals(p.getAmountPerShare())) {
							p.setAmountPerShare(dividend);
							tableViewer.update(p, new String[]{property});
							markDirty();
						}
					} else if (property.equals("currency")) {
						Currency currency =  getCurrencyByPos((Integer) value);
						p.setCurrency(currency);
						tableViewer.update(p, new String[]{property, "amount"});
						markDirty();
					} else if (property.equals("numberOfShares")) {
						if (StringUtils.isBlank((String) value)) {
							p.setQuantity(null);
						} else {
							p.setQuantity(PeanutsUtil.parseQuantity((String) value));
						}
						tableViewer.update(p, new String[]{property, "amount", "amountInDefaultCurrency"});
						markDirty();
					} else if (property.equals("amount")) {
						if (StringUtils.isBlank((String) value)) {
							p.setAmount(null);
						} else {
							p.setAmount(PeanutsUtil.parseCurrency((String) value));
						}
						tableViewer.update(p, new String[]{property, "amountInDefaultCurrency"});
						markDirty();
					} else if (property.equals("amountInDefaultCurrency")) {
						if (StringUtils.isBlank((String) value)) {
							p.setAmountInDefaultCurrency(null);
						} else {
							p.setAmountInDefaultCurrency(PeanutsUtil.parseCurrency((String) value));
						}
						tableViewer.update(p, new String[]{property});
						markDirty();
					} else if (property.equals("tax")) {
						if (StringUtils.isBlank((String) value)) {
							p.setTaxInDefaultCurrency(null);
						} else {
							p.setTaxInDefaultCurrency(PeanutsUtil.parseCurrency((String) value));
						}
						tableViewer.update(p, new String[]{property});
						markDirty();
					}
				} catch (ParseException | NumberFormatException e) {
					// Okay
				}
			}
		});
		ComboBoxCellEditor currencyCombo = new ComboBoxCellEditor(table, getCurrencyItems(), SWT.READ_ONLY);
		tableViewer.setCellEditors(new CellEditor[] {new DateCellEditor(table), new TextCellEditor(table), currencyCombo,
			new TextCellEditor(table), new TextCellEditor(table), new TextCellEditor(table), new TextCellEditor(table)});

		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		tableViewer.setLabelProvider(new DividendTableLabelProvider());
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.setInput(dividends);
		tableViewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				Dividend d1 = (Dividend) e1;
				Dividend d2 = (Dividend) e2;
				return d2.compareTo(d1);
			}
			@Override
			public boolean isSorterProperty(Object element, String property) {
				return "payDay".equals(property);
			}
		});

		MenuManager menuManager = new MenuManager();
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});
		table.setMenu(menuManager.createContextMenu(table));
		getSite().registerContextMenu(menuManager, tableViewer);
		getSite().setSelectionProvider(tableViewer);
	}

	private int getCurrencyPos(Currency currency) {
		String symbol = currency.getSymbol();
		String[] currencyItems = getCurrencyItems();
		return ArrayUtils.indexOf(currencyItems, symbol);
	}

	private Currency getCurrencyByPos(int pos) {
		return currencies.get(pos);
	}

	private String[] getCurrencyItems() {
		List<String> items = currencies.stream()
			.map(c -> c.getSymbol())
			.collect(Collectors.toList());
		return items.toArray(new String[items.size()]);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(new Action("New") {
			@Override
			public void run() {
				Currency currency = getCurrency();
				dividends.add(new Dividend(Day.today(), BigDecimal.ZERO, currency));
				tableViewer.refresh();
			}
		});

		final Action importAction = new Action("Import") {
			@Override
			public void run() {
				Clipboard cb = new Clipboard(Display.getDefault());
				String contents = (String) cb.getContents(TextTransfer.getInstance());
				String[] lines = contents.split("\\R");
				for (String line : lines) {
					String[] values = StringUtils.split(line, '\t');
					Day payDate = fromString(values[0]);
					BigDecimal dividendValue = new BigDecimal(StringUtils.replace(StringUtils.split(values[1])[1], ",", "."));

					Dividend dividend = new Dividend(payDate, dividendValue, getCurrency());
					if (values.length > 5) {
						BigDecimal amountInDefaultCurrency = new BigDecimal(StringUtils.replace(StringUtils.split(values[5])[0], ",", "."));
						dividend.setAmountInDefaultCurrency(amountInDefaultCurrency);
					}
					if (values.length > 6) {
						BigDecimal taxInDefaultCurrency = new BigDecimal(StringUtils.replace(StringUtils.split(values[6])[0], ",", "."));
						dividend.setTaxInDefaultCurrency(taxInDefaultCurrency);
					}
					dividends.add(dividend);
					tableViewer.refresh();
				}
				markDirty();
			}
		};
		manager.add(importAction);

		final Action duplicateAction = new Action("Duplicate") {
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				IStructuredSelection sel = (IStructuredSelection)tableViewer.getSelection();
				if (! sel.isEmpty()) {
					for (Iterator<Dividend> iter = sel.iterator(); iter.hasNext(); ) {
						Dividend entry = iter.next();
						Dividend dividend = new Dividend(entry);
						dividend.setPayDate(dividend.getPayDate().addYear(1));
						dividend.setAmountInDefaultCurrency(null);
						dividend.setTaxInDefaultCurrency(null);
						dividend.setQuantity(null);
						dividends.add(dividend);
					}
					tableViewer.refresh();
					markDirty();
				}
			}
		};
		duplicateAction.setEnabled(! ((IStructuredSelection)tableViewer.getSelection()).isEmpty());
		manager.add(duplicateAction);

		final Action toggleIncreaseAction = new Action("Toggle Increase") {
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				IStructuredSelection sel = (IStructuredSelection)tableViewer.getSelection();
				if (! sel.isEmpty()) {
					for (Iterator<Dividend> iter = sel.iterator(); iter.hasNext(); ) {
						Dividend entry = iter.next();
						entry.setIncrease(! entry.isIncrease());
					}
					tableViewer.refresh();
					markDirty();
				}
			}
		};
		manager.add(toggleIncreaseAction);
	}

	private InvestmentTransaction isBooked(Dividend dividend) {
		Day payDate = dividend.getPayDate();
		BigDecimal amount = dividend.getNettoAmountInDefaultCurrency();

		return securityReport
				.getTransactionsByDate(payDate.addDays(-1), payDate.addDays(14)).stream()
				.filter(InvestmentTransaction.class::isInstance)
				.map(InvestmentTransaction.class::cast)
				.filter(t -> t.getAmount().compareTo(amount) == 0)
				.findAny().orElse(null);
	}

	@Override
	public void setFocus() {
		tableViewer.getTable().setFocus();
	}

	private List<Dividend> cloneDividends(List<Dividend> dividends) {
		return dividends.stream()
			.map(d -> new Dividend(d))
			.collect(Collectors.toList());
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		getSecurity().updateDividends(cloneDividends(dividends));
		dirty = false;
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	public void markDirty() {
		dirty = true;
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}

	private Day fromString(String dayStr) {
		String[] split = StringUtils.split(dayStr, '.');
		if (split.length != 3) {
			throw new IllegalArgumentException("Format must be yyyy-MM-dd: '"+dayStr+"'");
		}
		int day = Integer.parseInt(split[0]);
		int month = Integer.parseInt(split[1]);
		int year = Integer.parseInt(split[2]);
		return Day.of(year, Month.of(month), day);
	}

	private Security getSecurity() {
		return ((SecurityEditorInput) getEditorInput()).getSecurity();
	}

	private Currency getCurrency() {
		FundamentalDatas fundamentalDatas = getSecurity().getFundamentalDatas();
		Currency currency = Currencies.getInstance().getDefaultCurrency();
		if (! fundamentalDatas.isEmpty()) {
			currency = fundamentalDatas.getCurrency();
		}
		return currency;
	}

	private List<Dividend> twelveMonthTrailingDividends(Dividend startDiv) {
		Day startDate = startDiv.getPayDate();
		return dividends.stream()
			.filter(d -> TWELVE_MONTH_RANGE.contains(d.getPayDate().delta(startDate)))
			.collect(Collectors.toList());
	}
	
	private BigDecimal getQuantity(Dividend entry) {
		if (entry.getQuantity() != null) {
			return entry.getQuantity();
		}
		securityInventory.setDate(entry.getPayDate());
		InventoryEntry inventoryEntry = securityInventory.getInventoryEntry(getSecurity());

		return inventoryEntry.getQuantity();
	}

	private BigDecimal getDividendYoc(List<Dividend> entries) {
		BigDecimal yoc = BigDecimal.ZERO;
		
		for (Dividend entry : entries) {
			securityInventory.setDate(entry.getPayDate());
			InventoryEntry inventoryEntry = securityInventory.getInventoryEntry(getSecurity());
			BigDecimal quantity = entry.getQuantity();
			if (quantity == null) {
				quantity = inventoryEntry.getQuantity();
			}
			if (quantity.compareTo(BigDecimal.ZERO) > 0
				&& inventoryEntry.getAvgPrice() != null
				&& inventoryEntry.getAvgPrice().compareTo(BigDecimal.ZERO) > 0) {

				BigDecimal amount = entry.getAmountInDefaultCurrency();
				if (amount == null) {
					amount = entry.getAmount();
					if (amount == null) {
						amount = getQuantity(entry).multiply(entry.getAmountPerShare());
					}
					CurrencyConverter converter = Activator.getDefault().getExchangeRates()
						.createCurrencyConverter(entry.getCurrency(), Currencies.getInstance().getDefaultCurrency());
					amount = converter.convert(amount, entry.getPayDate());
				}
				yoc = yoc.add(amount
					.divide(quantity, PeanutsUtil.MC)
					.divide(inventoryEntry.getAvgPrice(), PeanutsUtil.MC));
			}
		}
		return yoc;
	}

	public void deleteDividendEntries(List<Dividend> data) {
		if (dividends.removeAll(data)) {
			tableViewer.refresh();
			markDirty();
		}
	}

}
