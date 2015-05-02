package de.tomsplayground.peanuts.client.editors.security;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.widgets.CurrencyComboViewer;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.base.InventoryEntry;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;
import de.tomsplayground.peanuts.domain.currenncy.ExchangeRates;
import de.tomsplayground.peanuts.domain.fundamental.AvgFundamentalData;
import de.tomsplayground.peanuts.domain.fundamental.CurrencyAjustedFundamentalData;
import de.tomsplayground.peanuts.domain.fundamental.FundamentalData;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.domain.process.StockSplit;
import de.tomsplayground.peanuts.util.PeanutsUtil;
import de.tomsplayground.util.Day;

public class FundamentalDataEditorPart extends EditorPart {

	private TableViewer tableViewer;
	private final int colWidth[] = new int[13];
	private boolean dirty = false;
	private List<FundamentalData> fundamentalDatas;
	private IPriceProvider priceProvider;
	private InventoryEntry inventoryEntry;
	private CurrencyComboViewer currencyComboViewer;
	private CurrencyConverter currencyConverter;
	private List<Object> tableRows;

	private class FundamentalDataTableLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider {
		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		private FundamentalData getPreviousYear(FundamentalData data) {
			final int prevYear = data.getYear() -1;
			return Iterables.find(fundamentalDatas, new Predicate<FundamentalData>() {
				@Override
				public boolean apply(FundamentalData arg0) {
					return arg0.getYear() == prevYear;
				}
			}, null);
		}

		private BigDecimal epsGrowth(FundamentalData data) {
			FundamentalData previousYearData = getPreviousYear(data);
			if (previousYearData != null) {
				BigDecimal eps = data.getEarningsPerShare();
				BigDecimal prevEps = previousYearData.getEarningsPerShare();
				if (prevEps.signum() != 0) {
					return eps.divide(prevEps, new MathContext(10, RoundingMode.HALF_EVEN)).subtract(BigDecimal.ONE);
				}
			}
			return BigDecimal.ZERO;
		}

		private BigDecimal divGrowth(FundamentalData data) {
			FundamentalData previousYearData = getPreviousYear(data);
			if (previousYearData != null) {
				BigDecimal div = data.getDividende();
				BigDecimal prevDiv = previousYearData.getDividende();
				if (prevDiv.signum() != 0) {
					return div.divide(prevDiv, new MathContext(10, RoundingMode.HALF_EVEN)).subtract(BigDecimal.ONE);
				}
			}
			return BigDecimal.ZERO;
		}

		private BigDecimal adjustedEpsGrowth(FundamentalData data) {
			FundamentalData previousYearData = getPreviousYear(data);
			if (previousYearData != null) {
				BigDecimal eps = currencyAdjustedEPS(data);
				BigDecimal prevEps = currencyAdjustedEPS(previousYearData);
				if (prevEps.signum() != 0) {
					return eps.divide(prevEps, new MathContext(10, RoundingMode.HALF_EVEN)).subtract(BigDecimal.ONE);
				}
			}
			return BigDecimal.ZERO;
		}

		private BigDecimal adjustedDivGrowth(FundamentalData data) {
			FundamentalData previousYearData = getPreviousYear(data);
			if (previousYearData != null) {
				BigDecimal div = currencyAdjustedDiv(data);
				BigDecimal prevDiv = currencyAdjustedDiv(previousYearData);
				if (prevDiv.signum() != 0) {
					return div.divide(prevDiv, new MathContext(10, RoundingMode.HALF_EVEN)).subtract(BigDecimal.ONE);
				}
			}
			return BigDecimal.ZERO;
		}

		private BigDecimal currencyAdjustedEPS(FundamentalData data) {
			if (currencyConverter != null) {
				CurrencyAjustedFundamentalData currencyAjustedData = new CurrencyAjustedFundamentalData(data, currencyConverter);
				return currencyAjustedData.getEarningsPerShare();
			}
			return data.getEarningsPerShare();
		}

		private BigDecimal currencyAdjustedDiv(FundamentalData data) {
			if (currencyConverter != null) {
				CurrencyAjustedFundamentalData currencyAjustedData = new CurrencyAjustedFundamentalData(data, currencyConverter);
				return currencyAjustedData.getDividende();
			}
			return data.getDividende();
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof AvgFundamentalData) {
				AvgFundamentalData data = (AvgFundamentalData) element;
				switch (columnIndex) {
					case 0:
						return "Avg";
					case 6:
						return PeanutsUtil.formatPercent(data.getAvgEpsChange().subtract(BigDecimal.ONE));
					case 10:
						return PeanutsUtil.format(data.getAvgPE(), 1);
					default:
						return "";
				}
			} else {
				FundamentalData data = (FundamentalData) element;
				switch (columnIndex) {
					case 0:
						return String.valueOf(data.getYear());
					case 1:
						return PeanutsUtil.formatCurrency(data.getDividende(), null);
					case 2:
						return PeanutsUtil.formatPercent(divGrowth(data));
					case 3:
						return PeanutsUtil.formatCurrency(currencyAdjustedDiv(data), null);
					case 4:
						return PeanutsUtil.formatPercent(adjustedDivGrowth(data));
					case 5:
						return PeanutsUtil.formatCurrency(data.getEarningsPerShare(), null);
					case 6:
						return PeanutsUtil.formatPercent(epsGrowth(data));
					case 7:
						return PeanutsUtil.formatCurrency(currencyAdjustedEPS(data), null);
					case 8:
						return PeanutsUtil.formatPercent(adjustedEpsGrowth(data));
					case 9:
						return PeanutsUtil.formatQuantity(data.getDebtEquityRatio());
					case 10:
						if (currencyConverter != null) {
							CurrencyAjustedFundamentalData currencyAjustedData = new CurrencyAjustedFundamentalData(data, currencyConverter);
							return PeanutsUtil.format(currencyAjustedData.calculatePeRatio(priceProvider), 1);
						}
						return PeanutsUtil.format(data.calculatePeRatio(priceProvider), 1);
					case 11:
						if (currencyConverter != null) {
							CurrencyAjustedFundamentalData currencyAjustedData = new CurrencyAjustedFundamentalData(data, currencyConverter);
							return PeanutsUtil.formatPercent(currencyAjustedData.calculateDivYield(priceProvider));
						}
						return PeanutsUtil.formatPercent(data.calculateDivYield(priceProvider));
					case 12:
						if (inventoryEntry != null && data.getYear() == (new Day()).year) {
							if (currencyConverter != null) {
								CurrencyAjustedFundamentalData currencyAjustedData = new CurrencyAjustedFundamentalData(data, currencyConverter);
								return PeanutsUtil.formatPercent(currencyAjustedData.calculateYOC(inventoryEntry));
							}
							return PeanutsUtil.formatPercent(data.calculateYOC(inventoryEntry));
						} else {
							return "";
						}
					default:
						return "";
				}
			}
		}
		@Override
		public String getText(Object element) {
			if (element instanceof FundamentalData) {
				FundamentalData data = (FundamentalData) element;
				return String.valueOf(data.getYear());
			}
			return "";
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			return null;
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			if (element instanceof FundamentalData) {
				FundamentalData data = (FundamentalData) element;
				if (columnIndex == 2) {
					if (divGrowth(data).signum() == -1) {
						return Activator.getDefault().getColorProvider().get(Activator.RED);
					}
				}
				if (columnIndex == 4) {
					if (adjustedDivGrowth(data).signum() == -1) {
						return Activator.getDefault().getColorProvider().get(Activator.RED);
					}
				}
				if (columnIndex == 6) {
					if (epsGrowth(data).signum() == -1) {
						return Activator.getDefault().getColorProvider().get(Activator.RED);
					}
				}
				if (columnIndex == 8) {
					if (adjustedEpsGrowth(data).signum() == -1) {
						return Activator.getDefault().getColorProvider().get(Activator.RED);
					}
				}
			}
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
		final Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();

		Composite top = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		top.setLayout(layout);

		Composite metaComposite = new Composite(top, SWT.NONE);
		metaComposite.setLayout(new GridLayout());
		currencyComboViewer = new CurrencyComboViewer(metaComposite, false);

		tableViewer = new TableViewer(top, SWT.FULL_SELECTION);
		Table table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		int colNumber = 0;
		TableColumn col = new TableColumn(table, SWT.LEFT);
		col.setText("Year");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 80);
		col.setResizable(true);
		ViewerComparator comparator = new ViewerComparator() {
			@Override
			public int category(Object element) {
				if (element instanceof AvgFundamentalData) {
					return 99;
				}
				return 0;
			}
			@Override
			public boolean isSorterProperty(Object element, String property) {
				return "year".equals(property);
			}
		};
		tableViewer.setComparator(comparator);
		table.setSortColumn(col);
		table.setSortDirection(SWT.UP);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Dividende");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 70);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Change %");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 70);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Dividende "+security.getCurrency().getSymbol());
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 70);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Change %");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 70);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("EPS");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Change %");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 70);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("EPS "+security.getCurrency().getSymbol());
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 70);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Change %");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 70);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("D/E ratio");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("P/E ratio");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Div yield");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("YOC");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.setResizable(true);
		colNumber++;

		tableViewer.setColumnProperties(new String[] { "year", "div", "divgr", "div2", "div2gr",
			"EPS", "EPSgr", "EPS2", "EPS2gr", "deRatio", "peRatio", "divYield", "YOC"});
		tableViewer.setCellModifier(new ICellModifier() {

			@Override
			public boolean canModify(Object element, String property) {
				return Lists.newArrayList("year", "div", "EPS", "deRatio").contains(property);
			}

			@Override
			public Object getValue(Object element, String property) {
				FundamentalData p = (FundamentalData) element;
				if (property.equals("year")) {
					return String.valueOf(p.getYear());
				} else if (property.equals("div")) {
					return PeanutsUtil.formatCurrency(p.getDividende(), null);
				} else if (property.equals("EPS")) {
					return PeanutsUtil.formatCurrency(p.getEarningsPerShare(), null);
				} else if (property.equals("deRatio")) {
					return PeanutsUtil.formatCurrency(p.getDebtEquityRatio(), null);
				}
				return null;
			}

			@Override
			public void modify(Object element, String property, Object value) {
				FundamentalData p = (FundamentalData) ((TableItem) element).getData();
				try {
					if (property.equals("year")) {
						Integer newYear = Integer.valueOf((String) value);
						if (newYear.intValue() != p.getYear()) {
							p.setYear(newYear.intValue());
							tableViewer.update(p, new String[]{property});
							markDirty();
						}
					} else if (property.equals("div")) {
						BigDecimal v = PeanutsUtil.parseCurrency((String) value);
						if (! v.equals(p.getDividende())) {
							p.setDividende(v);
							tableViewer.update(p, new String[]{property});
							markDirty();
						}
					} else if (property.equals("EPS")) {
						BigDecimal v = PeanutsUtil.parseCurrency((String) value);
						if (! v.equals(p.getEarningsPerShare())) {
							p.setEarningsPerShare(v);
							tableViewer.update(p, new String[]{property});
							markDirty();
						}
					} else if (property.equals("deRatio")) {
						BigDecimal v = PeanutsUtil.parseCurrency((String) value);
						if (! v.equals(p.getDebtEquityRatio())) {
							p.setDebtEquityRatio(v);;
							tableViewer.update(p, new String[]{property});
							markDirty();
						}
					}
				} catch (ParseException e) {
					// Okay
				}
			}
		});
		tableViewer.setCellEditors(new CellEditor[] {new TextCellEditor(table), new TextCellEditor(table),
			new TextCellEditor(table), new TextCellEditor(table), new TextCellEditor(table), new TextCellEditor(table),
			new TextCellEditor(table), new TextCellEditor(table), new TextCellEditor(table), new TextCellEditor(table)});

		tableViewer.setLabelProvider(new FundamentalDataTableLabelProvider());
		tableViewer.setContentProvider(new ArrayContentProvider());
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		ImmutableList<StockSplit> stockSplits = Activator.getDefault().getAccountManager().getStockSplits(security);
		priceProvider = PriceProviderFactory.getInstance().getAdjustedPriceProvider(security, stockSplits);

		Inventory inventory = Activator.getDefault().getAccountManager().getFullInventory();
		for (InventoryEntry entry : inventory.getEntries()) {
			if (entry.getSecurity().equals(security)) {
				inventoryEntry = entry;
			}
		}

		fundamentalDatas = cloneFundamentalData(security.getFundamentalDatas());
		if (! fundamentalDatas.isEmpty()) {
			Currency currency = fundamentalDatas.get(0).getCurrency();
			currencyComboViewer.selectCurrency(currency);
			ExchangeRates exchangeRate = Activator.getDefault().getExchangeRate();
			currencyConverter = exchangeRate.createCurrencyConverter(currency, security.getCurrency());
		}

		tableRows = new ArrayList<Object>();
		tableRows.addAll(fundamentalDatas);
		tableRows.add(new AvgFundamentalData(fundamentalDatas, priceProvider, currencyConverter));
		tableViewer.setInput(tableRows);

		currencyComboViewer.getCombo().addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Currency selectedCurrency = currencyComboViewer.getSelectedCurrency();
				if (selectedCurrency.equals(security.getCurrency())) {
					currencyConverter = null;
				} else {
					ExchangeRates exchangeRate = Activator.getDefault().getExchangeRate();
					currencyConverter = exchangeRate.createCurrencyConverter(selectedCurrency, security.getCurrency());
				}
				tableViewer.refresh();
				markDirty();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				markDirty();
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

	private List<FundamentalData> cloneFundamentalData(Collection<FundamentalData> datas) {
		List<FundamentalData> fundamentalDatas = new ArrayList<FundamentalData>();
		for (FundamentalData d : datas) {
			fundamentalDatas.add(new FundamentalData(d));
		}
		Collections.sort(fundamentalDatas);
		return fundamentalDatas;
	}

	@Override
	public void setFocus() {
		tableViewer.getTable().setFocus();
	}

	protected void fillContextMenu(IMenuManager manager) {
		manager.add(new Action("New") {
			@Override
			public void run() {
				FundamentalData fundamentalData = new FundamentalData();
				fundamentalDatas.add(fundamentalData);
				tableRows.add(fundamentalData);
				tableViewer.add(fundamentalData);
				markDirty();
			}
		});
	}

	public void deleteFundamentalData(Collection<FundamentalData> data) {
		if (fundamentalDatas.removeAll(data)) {
			tableRows.removeAll(data);
			tableViewer.remove(data.toArray());
			markDirty();
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();
		List<FundamentalData> datas = cloneFundamentalData(fundamentalDatas);
		Currency selectedCurrency = currencyComboViewer.getSelectedCurrency();
		for (FundamentalData fundamentalData : datas) {
			fundamentalData.setCurrency(selectedCurrency);
		}
		security.setFundamentalDatas(datas);
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

}
