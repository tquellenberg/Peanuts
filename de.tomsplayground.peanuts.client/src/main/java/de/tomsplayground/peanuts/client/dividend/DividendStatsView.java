package de.tomsplayground.peanuts.client.dividend;

import java.awt.BasicStroke;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
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
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.jfree.ui.RectangleInsets;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.chart.PeanutsDrawingSupplier;
import de.tomsplayground.peanuts.client.util.UniqueAsyncExecution;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.base.InventoryEntry;
import de.tomsplayground.peanuts.domain.currenncy.Currencies;
import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;
import de.tomsplayground.peanuts.domain.dividend.Dividend;
import de.tomsplayground.peanuts.domain.dividend.DividendMonth;
import de.tomsplayground.peanuts.domain.dividend.DividendStats;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.domain.reporting.investment.AnalyzerFactory;
import de.tomsplayground.peanuts.domain.reporting.transaction.Report;
import de.tomsplayground.peanuts.util.PeanutsUtil;
import de.tomsplayground.util.Day;

public class DividendStatsView extends ViewPart {

	public static final String ID = "de.tomsplayground.peanuts.client.dividendStatsView";

	private TableViewer dividendStatsListViewer;

	private final int colWidth1[] = new int[7];
	private final int colWidth2[] = new int[7];

	private final PropertyChangeListener securityChangeListener = new UniqueAsyncExecution() {
		@Override
		public Display getDisplay() {
			return getSite().getShell().getDisplay();
		}
		@Override
		public void doit(PropertyChangeEvent evt, Display display) {
			List<DividendMonth> dividendStats = getDividendStats();
			Collections.reverse(dividendStats);
			dividendStatsListViewer.setInput(dividendStats);
		}
	};

	private TableViewer oneMonthListViewer;

	private Inventory fullInventory;

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
					return PeanutsUtil.formatQuantity(getQuantity(entry));
				case 5:
					if (entry.getAmount() != null) {
						return PeanutsUtil.formatCurrency(entry.getAmount(), entry.getCurrency());
					} else {
						return PeanutsUtil.formatCurrency(getQuantity(entry).multiply(entry.getAmountPerShare()), entry.getCurrency());
					}
				case 6:
					BigDecimal amount = entry.getAmountInDefaultCurrency();
					if (amount == null) {
						amount = entry.getAmount();
						if (amount == null) {
							amount = getQuantity(entry).multiply(entry.getAmountPerShare());
						}
						CurrencyConverter converter = Activator.getDefault().getExchangeRate()
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
			return null;
		}
	}

	private class DividendStatsListLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider {
		@Override
		public String getColumnText(Object element, int columnIndex) {
			DividendMonth divStats = (DividendMonth)element;
			switch (columnIndex) {
				case 0:
					return PeanutsUtil.formatDate(divStats.getMonth());
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
			if (divStats.getMonth().month == 11) {
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
		}
		Activator.getDefault().getAccountManager().getSecurities().stream()
			.forEach(s -> s.addPropertyChangeListener("dividends", securityChangeListener));

		Report report = new Report("temp");
		report.setAccounts(Activator.getDefault().getAccountManager().getAccounts());
		fullInventory = new Inventory(report, PriceProviderFactory.getInstance(), new Day(), new AnalyzerFactory());
	}

	@Override
	public void dispose() {
		Activator.getDefault().getAccountManager().getSecurities().stream()
			.forEach(s -> s.removePropertyChangeListener("dividends", securityChangeListener));
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
		TableColumn col = new TableColumn(table, SWT.LEFT);
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
		List<DividendMonth> dividendStats = getDividendStats();
		Collections.reverse(dividendStats);
		dividendStatsListViewer.setInput(dividendStats);

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
		JFreeChart chart = createChart();
		ChartComposite chartFrame = new ChartComposite(top, SWT.NONE, chart, true);
		chartFrame.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Left bottom: dividends of selected month
		createOneMonthTable(top);
	}

	private void updateOneMonthTable(Day month) {
		List<Dividend> dividends = Activator.getDefault().getAccountManager().getSecurities().stream()
			.flatMap(s -> s.getDividends().stream())
			.filter(d -> d.getPayDate().toMonth().equals(month))
			.sorted()
			.collect(Collectors.toList());
		oneMonthListViewer.setInput(dividends);
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
		col = new TableColumn(table, SWT.LEFT);
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

	private JFreeChart createChart() {
		StandardXYItemRenderer renderer = new StandardXYItemRenderer();
		XYDataset dataset = createTotalDataset(renderer);
		JFreeChart chart = ChartFactory.createXYLineChart(
			"Dividends", // title
			"Month", // x-axis label
			"Sum", // y-axis label
			dataset,
			PlotOrientation.VERTICAL,
			true, // create legend?
			true, // generate tooltips?
			false // generate URLs?
		);
		chart.setBackgroundPaint(java.awt.Color.white);

		XYPlot plot = (XYPlot) chart.getPlot();
		plot.setBackgroundPaint(PeanutsDrawingSupplier.BACKGROUND_PAINT);
		plot.setDomainGridlinePaint(PeanutsDrawingSupplier.GRIDLINE_PAINT);
		plot.setRangeGridlinePaint(PeanutsDrawingSupplier.GRIDLINE_PAINT);
		plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);
		plot.setDrawingSupplier(new PeanutsDrawingSupplier());
		plot.setRenderer(renderer);

		plot.getDomainAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());

		return chart;
	}

	private static final BasicStroke dash = new BasicStroke(1.0f,
        BasicStroke.CAP_BUTT,
        BasicStroke.JOIN_MITER,
        10.0f, new float[]{5.0f}, 0.0f);

	private XYDataset createTotalDataset(StandardXYItemRenderer renderer) {
		List<DividendMonth> dividendStats = getDividendStats();
		Day currentMonth = new Day();
		currentMonth = currentMonth.addDays(-currentMonth.day+1);
		int currentYear = 0;
		XYSeries timeSeries = null;
		List<XYSeries> series = new ArrayList<>();
		boolean future = false;
		for (DividendMonth dividendMonth : dividendStats) {
			if (dividendMonth.getMonth().year != currentYear) {
				currentYear = dividendMonth.getMonth().year;
				timeSeries = new XYSeries(getSeriesName(currentYear, future));
				series.add(timeSeries);
				if (future) {
					renderer.setSeriesStroke(series.size()-1, dash);
				} else {
					renderer.setSeriesStroke(series.size()-1, new BasicStroke(2.5f));
				}
			}
			if (! future && dividendMonth.getMonth().compareTo(currentMonth) >= 0) {
				timeSeries.add(new Integer(dividendMonth.getMonth().month+1), dividendMonth.getYearlyAmount());
				future = true;
				timeSeries = new XYSeries(getSeriesName(currentYear, future));
				series.add(timeSeries);
				renderer.setSeriesStroke(series.size()-1, dash);
			}
			if (future) {
				timeSeries.add(new Integer(dividendMonth.getMonth().month+1), dividendMonth.getFutureYearlyAmount());
			} else {
				timeSeries.add(new Integer(dividendMonth.getMonth().month+1), dividendMonth.getYearlyAmount());
			}
		}

		XYSeriesCollection dataset = new XYSeriesCollection();
		for (XYSeries timeSeries2 : series) {
			dataset.addSeries(timeSeries2);
		}
		return dataset;
	}

	private String getSeriesName(int year, boolean future) {
		return ""+ year + ((future)?" future":"");
	}

	private List<DividendMonth> getDividendStats() {
		return new DividendStats(Activator.getDefault().getAccountManager(),
			PriceProviderFactory.getInstance()).getDividendMonths();
	}

	private BigDecimal getQuantity(Dividend entry) {
		if (entry.getQuantity() != null) {
			return entry.getQuantity();
		}
		fullInventory.setDate(entry.getPayDate());
		InventoryEntry inventoryEntry = fullInventory.getInventoryEntry(entry.getSecurity());

		return inventoryEntry.getQuantity();
	}

	@Override
	public void setFocus() {
		dividendStatsListViewer.getTable().setFocus();
	}

}
