package de.tomsplayground.peanuts.client.editors.security;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Currency;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
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

import com.google.common.collect.Lists;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.widgets.DateCellEditor;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.base.InventoryEntry;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.currenncy.Currencies;
import de.tomsplayground.peanuts.domain.dividend.Dividend;
import de.tomsplayground.peanuts.domain.fundamental.FundamentalDatas;
import de.tomsplayground.peanuts.domain.reporting.transaction.Report;
import de.tomsplayground.peanuts.util.PeanutsUtil;
import de.tomsplayground.util.Day;

public class DividendEditorPart extends EditorPart {

	private boolean dirty;

	private TableViewer tableViewer;

	private final int colWidth[] = new int[15];

	private List<Dividend> dividends;

	private class DividendTableLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider {

		@Override
		public String getColumnText(Object element, int columnIndex) {
			Dividend entry = (Dividend) element;
			switch (columnIndex) {
				case 0:
					return PeanutsUtil.formatDate(entry.getPayDate());
				case 1:
					return PeanutsUtil.formatCurrency(entry.getAmountPerShare(), null);
				case 2:
					return entry.getCurrency().getSymbol();
				case 3:
					if (entry.getQuantity() != null) {
						return PeanutsUtil.formatQuantity(entry.getQuantity());
					} else {
						return PeanutsUtil.formatQuantity(getQuantity(entry.getPayDate()));
					}
				case 4:
					if (entry.getAmount() != null) {
						return PeanutsUtil.formatCurrency(entry.getAmount(), entry.getCurrency());
					} else {
						if (entry.getQuantity() != null) {
							return PeanutsUtil.formatCurrency(entry.getQuantity().multiply(entry.getAmountPerShare()), entry.getCurrency());
						} else {
							return PeanutsUtil.formatCurrency(getQuantity(entry.getPayDate()).multiply(entry.getAmountPerShare()), entry.getCurrency());
						}
					}
				case 5:
					return PeanutsUtil.formatCurrency(entry.getAmountInDefaultCurrency(), Currencies.getInstance().getDefaultCurrency());
				case 6:
					if (entry.getTaxInDefaultCurrency() != null) {
						return PeanutsUtil.formatCurrency(entry.getTaxInDefaultCurrency(), Currencies.getInstance().getDefaultCurrency());
					} else {
						return PeanutsUtil.formatCurrency(BigDecimal.ZERO, Currencies.getInstance().getDefaultCurrency());
					}
				case 7:
					return PeanutsUtil.formatCurrency(entry.getNettoAmountInDefaultCurrency(), Currencies.getInstance().getDefaultCurrency());
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
				case 6:
					return entry.getTaxInDefaultCurrency() == null ? Activator.getDefault().getColorProvider().get(Activator.INACTIVE_ROW) : null;
			}
			return null;
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
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
		col.setText("Netto "+Currencies.getInstance().getDefaultCurrency().getSymbol());
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.setResizable(true);
		colNumber++;

		tableViewer.setColumnProperties(new String[] { "payDay", "dividend", "currency", "numberOfShares", "amount",
			"amountInDefaultCurrency", "tax", "netto"});
		tableViewer.setCellModifier(new ICellModifier() {

			@Override
			public boolean canModify(Object element, String property) {
				return Lists.newArrayList("payDay", "dividend", "numberOfShares", "amount",
					"amountInDefaultCurrency", "tax").contains(property);
			}

			@Override
			public Object getValue(Object element, String property) {
				Dividend p = (Dividend) element;
				if (property.equals("payDay")) {
					return p.getPayDate();
				} else if (property.equals("dividend")) {
					return PeanutsUtil.formatCurrency(p.getAmountPerShare(), null);
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
		tableViewer.setCellEditors(new CellEditor[] {new DateCellEditor(table), new TextCellEditor(table), new TextCellEditor(table),
			new TextCellEditor(table), new TextCellEditor(table), new TextCellEditor(table), new TextCellEditor(table)});

		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		tableViewer.setLabelProvider(new DividendTableLabelProvider());
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.setInput(dividends);

		MenuManager menu = new MenuManager();
		menu.setRemoveAllWhenShown(true);
		menu.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});
		table.setMenu(menu.createContextMenu(table));
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(new Action("New") {
			@Override
			public void run() {
				Currency currency = getCurrency();
				dividends.add(new Dividend(new Day(), BigDecimal.ZERO, currency));
				tableViewer.refresh();
			}
		});

		final Action deleteAction = new Action("Delete") {
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				IStructuredSelection sel = (IStructuredSelection)tableViewer.getSelection();
				if (! sel.isEmpty()) {
					for (Iterator<Dividend> iter = sel.iterator(); iter.hasNext(); ) {
						Dividend entry = iter.next();
						dividends.remove(entry);
					}
					tableViewer.refresh();
				}
			}
		};
		deleteAction.setEnabled(! ((IStructuredSelection)tableViewer.getSelection()).isEmpty());
		manager.add(deleteAction);

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

					BigDecimal amountInDefaultCurrency = new BigDecimal(StringUtils.replace(StringUtils.split(values[5])[0], ",", "."));
					BigDecimal taxInDefaultCurrency = new BigDecimal(StringUtils.replace(StringUtils.split(values[6])[0], ",", "."));

					Dividend dividend = new Dividend(payDate, dividendValue, getCurrency());
					dividend.setAmountInDefaultCurrency(amountInDefaultCurrency);
					dividend.setTaxInDefaultCurrency(taxInDefaultCurrency);
					dividends.add(dividend);
					tableViewer.refresh();
				}
				markDirty();
			}
		};
		manager.add(importAction);
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
		Security security = getSecurity();
		security.getDividends().forEach(d -> security.removeDividend(d));
		dividends.forEach(d -> security.addDividend(new Dividend(d)));
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
		return new Day(year, month-1, day);
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

	private BigDecimal getQuantity(Day day) {
		Report report = new Report("temp");
		report.setAccounts(Activator.getDefault().getAccountManager().getAccounts());

		Inventory fullInventory = new Inventory(report, null);
		fullInventory.setDate(day);
		InventoryEntry inventoryEntry = fullInventory.getInventoryEntry(getSecurity());

		return inventoryEntry.getQuantity();
	}

}
