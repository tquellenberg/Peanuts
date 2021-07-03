package de.tomsplayground.peanuts.client.dividend;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.swt.ChartComposite;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.util.UniqueAsyncExecution;
import de.tomsplayground.peanuts.domain.currenncy.Currencies;
import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;
import de.tomsplayground.peanuts.domain.dividend.Dividend;
import de.tomsplayground.peanuts.domain.dividend.DividendMonth;
import de.tomsplayground.peanuts.domain.dividend.DividendStats;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class DividendStatsView extends ViewPart {

	public static final String ID = "de.tomsplayground.peanuts.client.dividendStatsView";

	private TableViewer dividendStatsListViewer;

	private final int colWidth1[] = new int[7];
	private final int colWidth2[] = new int[7];
	private final int colWidth3[] = new int[7];

	private final PropertyChangeListener dividendStatsChangeListener = new UniqueAsyncExecution() {
		@Override
		public Display getDisplay() {
			return getSite().getShell().getDisplay();
		}
		@Override
		public void doit(PropertyChangeEvent evt, Display display) {
			if (evt.getPropertyName().equals("dividends")) {
				updateTableData();
				dividendChart.updateChart();
			}
		}
	};

	private TableViewer oneMonthListViewer;

	private TableViewer yearlyListViewer;

	private DividendStats dividendStats;

	private DividendChart dividendChart;

	private class YearlyListLabelProvider extends LabelProvider implements ITableLabelProvider {
		@Override
		public String getColumnText(Object element, int columnIndex) {
			DividendMonth divStats = (DividendMonth)element;
			switch (columnIndex) {
				case 0:
					return String.valueOf(divStats.getMonth().getYear());
				case 1:
					return PeanutsUtil.formatCurrency(getSum(divStats), Currencies.getInstance().getDefaultCurrency());
				case 2:
					@SuppressWarnings("unchecked") List<DividendMonth> input = (List<DividendMonth>) yearlyListViewer.getInput();
					int pos = input.indexOf(divStats);
					if (pos < 1) {
						return "";
					} else {
						BigDecimal lastYearSum = getSum(input.get(pos-1));
						if (lastYearSum.signum() != 0) {
							BigDecimal change = getSum(divStats).divide(lastYearSum, PeanutsUtil.MC).subtract(BigDecimal.ONE);
							return PeanutsUtil.formatPercent(change);
						} else {
							return "";
						}
					}
				case 3:
					return PeanutsUtil.formatCurrency(divStats.getYearlyNetto(), Currencies.getInstance().getDefaultCurrency());
				case 4:
					BigDecimal brutto = divStats.getYearlyAmount();
					BigDecimal netto = divStats.getYearlyNetto();
					if (brutto.signum() > 0 && netto.signum() > 0) {
						BigDecimal taxQuote = brutto.subtract(netto).divide(brutto, PeanutsUtil.MC);
						return PeanutsUtil.formatPercent(taxQuote);
					} else {
						return null;
					}
				case 5:
					return PeanutsUtil.formatCurrency(divStats.getInvestedAvg(), Currencies.getInstance().getDefaultCurrency());
				case 6:
					if (divStats.getInvestedAvg().signum() != 0) {
						BigDecimal quote = getSum(divStats).divide(divStats.getInvestedAvg(), PeanutsUtil.MC);
						return PeanutsUtil.formatPercent(quote);
					}
			}
			return null;
		}

		private BigDecimal getSum(DividendMonth divStats) {
			if (divStats.getFutureYearlyAmount() != null && divStats.getFutureYearlyAmount().compareTo(BigDecimal.ZERO) > 0) {
				return divStats.getFutureYearlyAmount();
			}
			return divStats.getYearlyAmount();
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
	}

	private class OneMonthListLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider {

		@Override
		public String getColumnText(Object element, int columnIndex) {
			Dividend entry = (Dividend)element;
			switch (columnIndex) {
				case 0:
					return PeanutsUtil.formatDate(entry.getPayDate());
				case 1:
					return entry.getSecurity().getName();
				case 2:
					return PeanutsUtil.format(entry.getAmountPerShare(), 4);
				case 3:
					return entry.getCurrency().getSymbol();
				case 4:
					return PeanutsUtil.formatQuantity(dividendStats.getQuantity(entry));
				case 5:
					if (entry.getAmount() != null) {
						return PeanutsUtil.formatCurrency(entry.getAmount(), entry.getCurrency());
					} else {
						return PeanutsUtil.formatCurrency(dividendStats.getQuantity(entry).multiply(entry.getAmountPerShare()), entry.getCurrency());
					}
				case 6:
					BigDecimal amount = entry.getAmountInDefaultCurrency();
					if (amount == null) {
						amount = entry.getAmount();
						if (amount == null) {
							amount = dividendStats.getQuantity(entry).multiply(entry.getAmountPerShare());
						}
						CurrencyConverter converter = Activator.getDefault().getExchangeRates()
							.createCurrencyConverter(entry.getCurrency(), Currencies.getInstance().getDefaultCurrency());
						amount = converter.convert(amount, entry.getPayDate());
					}
					return PeanutsUtil.formatCurrency(amount, Currencies.getInstance().getDefaultCurrency());
			}
			return null;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			Dividend entry = (Dividend) element;
			switch (columnIndex) {
				case 4:
					return entry.getQuantity() == null ? Activator.getDefault().getColorProvider().get(Activator.INACTIVE_ROW) : null;
				case 5:
					return entry.getAmount() == null ? Activator.getDefault().getColorProvider().get(Activator.INACTIVE_ROW) : null;
				case 6:
					return entry.getAmountInDefaultCurrency() == null ? Activator.getDefault().getColorProvider().get(Activator.INACTIVE_ROW) : null;
			}
			return null;
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			Dividend entry = (Dividend) element;
			if (entry.isIncrease()) {
				return Activator.getDefault().getColorProvider().get(Activator.GREEN_BG);
			}
			return null;
		}
	}

	private class DividendStatsListLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider {
		@Override
		public String getColumnText(Object element, int columnIndex) {
			DividendMonth divStats = (DividendMonth)element;
			switch (columnIndex) {
				case 0:
					return PeanutsUtil.formatMonth(divStats.getMonth());
				case 1:
					return PeanutsUtil.formatCurrency(divStats.getAmountInDefaultCurrency(), Currencies.getInstance().getDefaultCurrency());
				case 2:
					return PeanutsUtil.formatCurrency(divStats.getYearlyAmount(), Currencies.getInstance().getDefaultCurrency());
				case 3:
					return PeanutsUtil.formatCurrency(divStats.getNettoInDefaultCurrency(), Currencies.getInstance().getDefaultCurrency());
				case 4:
					return PeanutsUtil.formatCurrency(divStats.getYearlyNetto(), Currencies.getInstance().getDefaultCurrency());
				case 5:
					BigDecimal futureAmountInDefaultCurrency = divStats.getFutureAmountInDefaultCurrency();
					if (futureAmountInDefaultCurrency.signum() == 0) {
						return "";
					}
					return PeanutsUtil.formatCurrency(futureAmountInDefaultCurrency, Currencies.getInstance().getDefaultCurrency());
				case 6:
					futureAmountInDefaultCurrency = divStats.getFutureAmountInDefaultCurrency();
					if (futureAmountInDefaultCurrency.signum() == 0) {
						return "";
					}
					return PeanutsUtil.formatCurrency(divStats.getFutureYearlyAmount(), Currencies.getInstance().getDefaultCurrency());

				default:
					break;
			}
			return null;
		}
		@Override
		public Color getForeground(Object element, int columnIndex) {
			return null;
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			DividendMonth divStats = (DividendMonth)element;
			if (divStats.getMonth().getMonth() == Month.DECEMBER) {
				return Activator.getDefault().getColorProvider().get(Activator.GRAY_BG);
			}
			return null;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		if (memento != null) {
			for (int i = 0; i < colWidth1.length; i++ ) {
				Integer width = memento.getInteger("col_1." + i);
				if (width != null) {
					colWidth1[i] = width.intValue();
				}
			}
			for (int i = 0; i < colWidth2.length; i++ ) {
				Integer width = memento.getInteger("col_2." + i);
				if (width != null) {
					colWidth2[i] = width.intValue();
				}
			}
			for (int i = 0; i < colWidth3.length; i++ ) {
				Integer width = memento.getInteger("col_3." + i);
				if (width != null) {
					colWidth3[i] = width.intValue();
				}
			}
		}

		dividendStats = new DividendStats(Activator.getDefault().getAccountManager(), PriceProviderFactory.getInstance());
		dividendStats.addPropertyChangeListener(dividendStatsChangeListener);

		dividendChart = new DividendChart(dividendStats);
	}

	@Override
	public void dispose() {
		dividendStats.removePropertyChangeListener(dividendStatsChangeListener);
		dividendStats.dispose();
		super.dispose();
	}

	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		TableColumn[] columns = dividendStatsListViewer.getTable().getColumns();
		for (int i = 0; i < columns.length; i++ ) {
			TableColumn tableColumn = columns[i];
			memento.putInteger("col_1." + i, tableColumn.getWidth());
		}
		columns = oneMonthListViewer.getTable().getColumns();
		for (int i = 0; i < columns.length; i++ ) {
			TableColumn tableColumn = columns[i];
			memento.putInteger("col_2." + i, tableColumn.getWidth());
		}
		columns = yearlyListViewer.getTable().getColumns();
		for (int i = 0; i < columns.length; i++ ) {
			TableColumn tableColumn = columns[i];
			memento.putInteger("col_3." + i, tableColumn.getWidth());
		}
	}

	@Override
	public void createPartControl(Composite parent) {

		Composite top = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, true);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		top.setLayout(layout);

		// Left top table: every row is a month
		dividendStatsListViewer = new TableViewer(top, SWT.SINGLE | SWT.FULL_SELECTION);
		Table table = dividendStatsListViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		ColumnViewerToolTipSupport.enableFor(dividendStatsListViewer);
		// must be called  before tableViewerColumn.setLabelProvider
		dividendStatsListViewer.setLabelProvider(new DividendStatsListLabelProvider());

		int colNum = 0;
		TableColumn col = new TableColumn(table, SWT.RIGHT);
		col.setText("Month");
		col.setWidth((colWidth1[colNum] > 0) ? colWidth1[colNum] : 300);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Amount");
		col.setWidth((colWidth1[colNum] > 0) ? colWidth1[colNum] : 150);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Sum in year");
		col.setWidth((colWidth1[colNum] > 0) ? colWidth1[colNum] : 150);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Netto");
		col.setWidth((colWidth1[colNum] > 0) ? colWidth1[colNum] : 150);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Netto sum in year");
		col.setWidth((colWidth1[colNum] > 0) ? colWidth1[colNum] : 150);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Future Amount");
		col.setWidth((colWidth1[colNum] > 0) ? colWidth1[colNum] : 150);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Future Sum");
		col.setWidth((colWidth1[colNum] > 0) ? colWidth1[colNum] : 150);
		col.setResizable(true);
		colNum++;

		dividendStatsListViewer.setContentProvider(new ArrayContentProvider());

		table.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TableItem[] selection = table.getSelection();
				if (selection != null && selection.length > 0) {
					DividendMonth diviMonth = (DividendMonth) selection[0].getData();
					updateOneMonthTable(diviMonth.getMonth());
				}
			}
		});

		// Right: chart
		JFreeChart chart = dividendChart.createChart();
		ChartComposite chartFrame = new ChartComposite(top, SWT.NONE, chart, true);
		chartFrame.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Left bottom: dividends of selected month
		createOneMonthTable(top);

		// Right bottom: yearly statistics
		createYearlyTable(top);

		updateTableData();
	}

	private void createYearlyTable(Composite top) {
		int colNum;
		TableColumn col;

		yearlyListViewer = new TableViewer(top, SWT.MULTI | SWT.FULL_SELECTION | SWT.SINGLE);
		Table table = yearlyListViewer.getTable();
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.heightHint = 200;
		gridData.minimumHeight = 200;
		table.setLayoutData(gridData);

		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		ColumnViewerToolTipSupport.enableFor(yearlyListViewer);
		// must be called  before tableViewerColumn.setLabelProvider
		yearlyListViewer.setLabelProvider(new YearlyListLabelProvider());
		yearlyListViewer.setContentProvider(new ArrayContentProvider());
		yearlyListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				DividendMonth firstElement = (DividendMonth) event.getStructuredSelection().getFirstElement();
				if (firstElement != null) {
					dividendChart.selectYear(firstElement.getMonth().getYear());
				} else {
					dividendChart.deselectYear();
				}
			}
		});

		colNum = 0;
		col = new TableColumn(table, SWT.LEFT);
		col.setText("Year");
		col.setWidth((colWidth3[colNum] > 0) ? colWidth3[colNum] : 80);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Sum");
		col.setWidth((colWidth3[colNum] > 0) ? colWidth3[colNum] : 100);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Change %");
		col.setWidth((colWidth3[colNum] > 0) ? colWidth3[colNum] : 80);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Netto");
		col.setWidth((colWidth3[colNum] > 0) ? colWidth3[colNum] : 80);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Tax %");
		col.setWidth((colWidth3[colNum] > 0) ? colWidth3[colNum] : 80);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Invested avg");
		col.setWidth((colWidth3[colNum] > 0) ? colWidth3[colNum] : 80);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Quote");
		col.setWidth((colWidth3[colNum] > 0) ? colWidth3[colNum] : 80);
		col.setResizable(true);
		colNum++;
	}

	private void updateOneMonthTable(YearMonth month) {
		List<Dividend> dividends = dividendStats.getDividends(month);
		Collections.sort(dividends);
		oneMonthListViewer.setInput(dividends);
		oneMonthListViewer.getTable().redraw();
	}

	private void createOneMonthTable(Composite top) {
		int colNum;
		TableColumn col;

		oneMonthListViewer = new TableViewer(top, SWT.MULTI | SWT.FULL_SELECTION);
		Table table = oneMonthListViewer.getTable();
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.heightHint = 200;
		gridData.minimumHeight = 200;
		table.setLayoutData(gridData);

		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		ColumnViewerToolTipSupport.enableFor(oneMonthListViewer);
		// must be called  before tableViewerColumn.setLabelProvider
		oneMonthListViewer.setLabelProvider(new OneMonthListLabelProvider());

		colNum = 0;
		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Date");
		col.setWidth((colWidth2[colNum] > 0) ? colWidth2[colNum] : 80);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.LEFT);
		col.setText("Security");
		col.setWidth((colWidth2[colNum] > 0) ? colWidth2[colNum] : 100);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Dividend");
		col.setWidth((colWidth2[colNum] > 0) ? colWidth2[colNum] : 80);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Currency");
		col.setWidth((colWidth2[colNum] > 0) ? colWidth2[colNum] : 70);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("# of shares");
		col.setWidth((colWidth2[colNum] > 0) ? colWidth2[colNum] : 70);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Dividend sum");
		col.setWidth((colWidth2[colNum] > 0) ? colWidth2[colNum] : 100);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Dividend sum "+Currencies.getInstance().getDefaultCurrency().getSymbol());
		col.setWidth((colWidth2[colNum] > 0) ? colWidth2[colNum] : 100);
		col.setResizable(true);
		colNum++;

		oneMonthListViewer.setContentProvider(new ArrayContentProvider());
		oneMonthListViewer.setInput(new ArrayList<>());
	}

	private void updateTableData() {
		List<DividendMonth> dividendMonths = dividendStats.getDividendMonths();
		Collections.reverse(dividendMonths);
		dividendStatsListViewer.setInput(dividendMonths);

		List<DividendMonth> yearlyStats = dividendMonths.stream()
			.filter(s -> s.getMonth().getMonth() == Month.DECEMBER)
			.sorted()
			.collect(Collectors.toList());
		yearlyListViewer.setInput(yearlyStats);
	}

	@Override
	public void setFocus() {
		dividendStatsListViewer.getTable().setFocus();
	}

}
