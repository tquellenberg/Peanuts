package de.tomsplayground.peanuts.client.editors.account;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.swt.ChartComposite;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.chart.JFreeChartFonts;
import de.tomsplayground.peanuts.client.chart.PeanutsDrawingSupplier;
import de.tomsplayground.peanuts.client.chart.TimeChart;
import de.tomsplayground.peanuts.client.util.UniqueAsyncExecution;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.domain.reporting.transaction.DateIterator;
import de.tomsplayground.peanuts.domain.reporting.transaction.TimeIntervalReport;
import de.tomsplayground.peanuts.domain.statistics.AccountValueData;

public class ValueChartEditorPart extends EditorPart {

	private static final String CHART_TYPE = "chartType";

	private Combo displayType;
	private boolean dirty;
	private TimeChart timeChart;

	private TimeIntervalReport intervalReport;

	private ChartComposite chartFrame;

	private final PropertyChangeListener changeListener = new UniqueAsyncExecution() {
		@Override
		public void doit(PropertyChangeEvent evt, Display display) {
			if (!displayType.isDisposed()) {
				String type = displayType.getItem(displayType.getSelectionIndex());
				chartFrame.setChart(createChart(type));
				chartFrame.redraw();
			}
		}

		@Override
		public Display getDisplay() {
			return  getSite().getWorkbenchWindow().getWorkbench().getDisplay();
		}
	};

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if ( !(input instanceof AccountEditorInput)) {
			throw new PartInitException("Invalid Input: Must be AccountEditorInput");
		}
		setSite(site);
		setInput(input);
		setPartName(input.getName());
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite body = new Composite(parent, SWT.NONE);
		body.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		body.setLayout(new GridLayout());

		Account account = getAccount();
		intervalReport = new TimeIntervalReport(account, TimeIntervalReport.Interval.DAY, 
				PriceProviderFactory.getInstance(account.getCurrency(), Activator.getDefault().getExchangeRates()), 
				Activator.getDefault().getAccountManager());
		intervalReport.addPropertyChangeListener(changeListener);

		String chartType = StringUtils.defaultString(account.getConfigurationValue(CHART_TYPE), "all");
		JFreeChart chart = createChart(chartType);
		chartFrame = new ChartComposite(body, SWT.NONE, chart, true);
		chartFrame.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		displayType = new Combo(body, SWT.READ_ONLY);
		for (TimeChart.RANGE r : TimeChart.RANGE.values()) {
			displayType.add(r.getName());
		}
		displayType.setText(chartType);
		displayType.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				Combo c = (Combo)e.getSource();
				String type = c.getItem(c.getSelectionIndex());
				timeChart.setChartType(type, intervalReport.getEnd());
				dirty = true;
				firePropertyChange(IEditorPart.PROP_DIRTY);
			}
		});
	}

	private Account getAccount() {
		return ((AccountEditorInput) getEditorInput()).getAccount();
	}

	@Override
	public void dispose() {
		intervalReport.removePropertyChangeListener(changeListener);
		intervalReport.dispose();
		super.dispose();
	}

	private void todayMarker(XYPlot plot) {
		de.tomsplayground.peanuts.util.Day day = de.tomsplayground.peanuts.util.Day.today();
		long x = new Day(day.day, day.getMonth().getValue(), day.year).getFirstMillisecond();
		ValueMarker valueMarker = new ValueMarker(x);
		valueMarker.setAlpha(0.50f);
		valueMarker.setLabelTextAnchor(TextAnchor.BOTTOM_RIGHT);
		valueMarker.setLabelAnchor(RectangleAnchor.BOTTOM_LEFT);
		valueMarker.setLabel("Today");
		plot.addDomainMarker(valueMarker);
	}

	private JFreeChart createChart(String chartType) {
		CombinedDomainXYPlot plot = new CombinedDomainXYPlot(new DateAxis("Date"));
		JFreeChart chart = new JFreeChart(getEditorInput().getName(), plot);
		chart.setBackgroundPaint(Color.white);

		TimeSeriesCollection dataset = createTotalDataset();
		StandardXYItemRenderer standardXYItemRenderer = new StandardXYItemRenderer();
		standardXYItemRenderer.setDefaultToolTipGenerator(StandardXYToolTipGenerator.getTimeSeriesInstance());
		NumberAxis numberAxis1 = new NumberAxis("Value");
		numberAxis1.setTickLabelFont(JFreeChartFonts.getTickLabelFont());
		XYPlot subplot1 = new XYPlot(dataset, null,  numberAxis1, standardXYItemRenderer);
		plot.add(subplot1, 70);
		subplot1.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 0.0));
		subplot1.setDomainCrosshairVisible(true);
		subplot1.setRangeCrosshairVisible(true);
		todayMarker(subplot1);

		XYAreaRenderer xyAreaRenderer = new XYAreaRenderer();
		xyAreaRenderer.setDefaultToolTipGenerator(StandardXYToolTipGenerator.getTimeSeriesInstance());
		NumberAxis numberAxis2 = new NumberAxis("Gain/Loss");
		numberAxis2.setTickLabelFont(JFreeChartFonts.getTickLabelFont());
		XYPlot subplot2 = new XYPlot(createGainLossDataset(), null, numberAxis2, xyAreaRenderer);
		subplot2.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 0.0));
		subplot2.setDomainCrosshairVisible(true);
		subplot2.setRangeCrosshairVisible(true);
		plot.add(subplot2, 30);

		plot.setDrawingSupplier(new PeanutsDrawingSupplier());

		DateAxis axis = (DateAxis) plot.getDomainAxis();
		axis.setDateFormatOverride(new SimpleDateFormat("MMM yyyy"));
		axis.setTickLabelFont(JFreeChartFonts.getTickLabelFont());

		timeChart = new TimeChart(chart, dataset);
		timeChart.setChartType(chartType, intervalReport.getEnd());

		return chart;
	}

	private TimeSeriesCollection createGainLossDataset() {
		List<BigDecimal> values = intervalReport.getValues();
		DateIterator dateIterator = intervalReport.dateIterator();
		List<BigDecimal> inventoryValues = intervalReport.getInventoryValues();
		List<BigDecimal> investmentValues = intervalReport.getInvestmentValues();
		TimeSeries s3 = new TimeSeries("Saldo");
		BigDecimal sum = BigDecimal.ZERO;
		Iterator<BigDecimal> iterator1 = inventoryValues.iterator();
		Iterator<BigDecimal> iterator2 = investmentValues.iterator();
		for (BigDecimal v : values) {
			sum = sum.add(v);
			de.tomsplayground.peanuts.util.Day d = dateIterator.next();
			Day day = new Day(d.day, d.getMonth().getValue(), d.year);
			BigDecimal v1 = sum.add(iterator1.next());
			BigDecimal v2 = iterator2.next();
			s3.add(day, v1.subtract(v2));
		}
		TimeSeriesCollection dataset = new TimeSeriesCollection();
		dataset.addSeries(s3);

		return dataset;
	}

	private TimeSeriesCollection createTotalDataset() {
		List<BigDecimal> values = intervalReport.getValues();
		DateIterator dateIterator = intervalReport.dateIterator();
		List<BigDecimal> inventoryValues = intervalReport.getInventoryValues();
		List<BigDecimal> investmentValues = intervalReport.getInvestmentValues();
		TimeSeries s1 = new TimeSeries(getEditorInput().getName());
		TimeSeries s2 = new TimeSeries("Invested sum");
		BigDecimal sum = BigDecimal.ZERO;
		Iterator<BigDecimal> iterator1 = inventoryValues.iterator();
		Iterator<BigDecimal> iterator2 = investmentValues.iterator();
		for (BigDecimal v : values) {
			sum = sum.add(v);
			de.tomsplayground.peanuts.util.Day d = dateIterator.next();
			Day day = new Day(d.day, d.getMonth().getValue(), d.year);
			BigDecimal v1 = sum.add(iterator1.next());
			s1.add(day, v1);
			BigDecimal v2 = iterator2.next();
			s2.add(day, v2);
		}
		TimeSeriesCollection dataset = new TimeSeriesCollection();
		dataset.addSeries(s1);
		dataset.addSeries(s2);

		return dataset;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		String type = displayType.getItem(displayType.getSelectionIndex());
		getAccount().putConfigurationValue(CHART_TYPE, type);
		dirty = false;
	}

	@Override
	public void doSaveAs() {
		// nothing to do
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void setFocus() {
		displayType.setFocus();
	}

	public AccountValueData getAccountValueData() {
		AccountValueData accountValueData = new AccountValueData();
		List<BigDecimal> values = intervalReport.getValues();
		DateIterator dateIterator = intervalReport.dateIterator();
		List<BigDecimal> inventoryValues = intervalReport.getInventoryValues();
		List<BigDecimal> investmentValues = intervalReport.getInvestmentValues();
		BigDecimal sum = BigDecimal.ZERO;
		Iterator<BigDecimal> valueIter = inventoryValues.iterator();
		Iterator<BigDecimal> investmentIter = investmentValues.iterator();
		for (BigDecimal v : values) {
			sum = sum.add(v);
			de.tomsplayground.peanuts.util.Day d = dateIterator.next();
			BigDecimal v1 = sum.add(valueIter.next());
			BigDecimal v2 = investmentIter.next();
			accountValueData.add(d, v1, v2);
		}
		return accountValueData;
	}

}
