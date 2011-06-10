package de.tomsplayground.peanuts.client.editors.report;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
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
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Day;
import org.jfree.data.time.Month;
import org.jfree.data.time.Quarter;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.Year;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.jfree.ui.RectangleInsets;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.chart.PeanutsDrawingSupplier;
import de.tomsplayground.peanuts.client.chart.TimeChart;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.domain.reporting.forecast.Forecast;
import de.tomsplayground.peanuts.domain.reporting.transaction.DateIterator;
import de.tomsplayground.peanuts.domain.reporting.transaction.Report;
import de.tomsplayground.peanuts.domain.reporting.transaction.TimeIntervalReport;
import de.tomsplayground.peanuts.domain.reporting.transaction.TimeIntervalReport.Interval;

public class ChartEditorPart extends EditorPart {
	
	private static final String CHART_TYPE = "chartType";
	private static final String TIMERANGE = "timerange";
	
	private boolean dirty = false;

	private Combo displayType;

	private ChartComposite chartFrame;

	private final PropertyChangeListener reportChangeListener;

	private Combo displayTimerange;
	private TimeChart timeChart;

	public ChartEditorPart() {
		reportChangeListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				String type = displayType.getItem(displayType.getSelectionIndex());
				chartFrame.setChart(createChart(type));
				chartFrame.redraw();
			}
		};
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if ( !(input instanceof ReportEditorInput)) {
			throw new PartInitException("Invalid Input: Must be ReportEditorInput");
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

		String chartType = StringUtils.defaultString(getReport().getConfigurationValue(CHART_TYPE), "in total");
		JFreeChart chart = createChart(chartType);
		chartFrame = new ChartComposite(body, SWT.NONE, chart, true);
		chartFrame.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		displayType = new Combo(body, SWT.READ_ONLY);
		displayType.add("in total");
		displayType.add("per month");
		displayType.add("per quarter");
		displayType.add("per year");
		displayType.setText(chartType);
		displayType.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				Combo c = (Combo)e.getSource();
				String type = c.getItem(c.getSelectionIndex());
				chartFrame.setChart(createChart(type));
				chartFrame.redraw();
				displayTimerange.setText("all");
				dirty = true;
				firePropertyChange(IEditorPart.PROP_DIRTY);
			}
		});
		
		String timerange = StringUtils.defaultString(getReport().getConfigurationValue(TIMERANGE), "all");
		displayTimerange = new Combo(body, SWT.READ_ONLY);
		displayTimerange.add("all");
		displayTimerange.add("one year");
		displayTimerange.add("this year");
		displayTimerange.add("6 month");
		displayTimerange.add("1 month");
		displayTimerange.setText(timerange);
		displayTimerange.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				Combo c = (Combo)e.getSource();
				String type = c.getItem(c.getSelectionIndex());
				if (timeChart != null) {
					timeChart.setChartType(type);
					dirty = true;
					firePropertyChange(IEditorPart.PROP_DIRTY);
				}
			}
		});		
		
		Report report = getReport();
		report.addPropertyChangeListener(reportChangeListener);
	}

	private Report getReport() {
		return ((ReportEditorInput)getEditorInput()).getReport();
	}

	@Override
	public void dispose() {
		Report report = getReport();
		report.removePropertyChangeListener(reportChangeListener);		
		super.dispose();
	}
	
	private JFreeChart createChart(String type) {
		JFreeChart chart;
		SimpleDateFormat axisDateFormat;
		if (type.equals("in total")) {
			TimeSeriesCollection dataset = createTotalDataset();
			chart = ChartFactory.createTimeSeriesChart(
					getEditorInput().getName(), // title
					"Date", // x-axis label
					"Price", // y-axis label
					dataset, // data
					false, // create legend?
					true, // generate tooltips?
					false // generate URLs?
				);
			timeChart = new TimeChart(chart, dataset);
			axisDateFormat = new SimpleDateFormat("MMM-yyyy");
		} else {
			// per month / year
			TimeSeriesCollection dataset;
			if (type.equals("per month")) {
				dataset = createDeltaDataset(TimeIntervalReport.Interval.MONTH, Month.class);
				axisDateFormat = new SimpleDateFormat("MMM-yyyy");
			} else if (type.equals("per quarter")) {
				dataset = createDeltaDataset(TimeIntervalReport.Interval.QUARTER, Quarter.class);
				axisDateFormat = new SimpleDateFormat("yyyy");
			} else {
				dataset = createDeltaDataset(TimeIntervalReport.Interval.YEAR, Year.class);
				axisDateFormat = new SimpleDateFormat("yyyy");
			}
			chart = ChartFactory.createXYBarChart(
					getEditorInput().getName(), // title
					"Date", // x-axis label
					true,
					"Price", // y-axis label
					dataset, // data
					PlotOrientation.VERTICAL,
					false, // create legend?
					true, // generate tooltips?
					false // generate URLs?
				);
			timeChart = new TimeChart(chart, dataset);
		}
		
		chart.setBackgroundPaint(Color.white);

		XYPlot plot = (XYPlot) chart.getPlot();
		plot.setBackgroundPaint(PeanutsDrawingSupplier.BACKGROUND_PAINT);
		plot.setDomainGridlinePaint(PeanutsDrawingSupplier.GRIDLINE_PAINT);
		plot.setRangeGridlinePaint(PeanutsDrawingSupplier.GRIDLINE_PAINT);
		plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);
		plot.setDrawingSupplier(new PeanutsDrawingSupplier());

		DateAxis axis = (DateAxis) plot.getDomainAxis();
		axis.setDateFormatOverride(axisDateFormat);

		return chart;
	}

	private TimeSeriesCollection createTotalDataset() {
		Report report = getReport();
		TimeIntervalReport intervalReport = new TimeIntervalReport(report, TimeIntervalReport.Interval.DAY, PriceProviderFactory.getInstance());
		List<BigDecimal> values = intervalReport.getValues();
		List<BigDecimal> inventoryValues = intervalReport.getInventoryValues();
		TimeSeries s1 = new TimeSeries(getEditorInput().getName(), Day.class);
		List<Forecast> forecasts = new ArrayList<Forecast>(Activator.getDefault().getAccountManager().getForecasts());
		for (Iterator<Forecast> iter = forecasts.iterator(); iter.hasNext(); ) {
			Forecast f = iter.next();
			if (! f.isConnected(report))
				iter.remove();
		}
		TimeSeries forecastSeries[] = new TimeSeries[forecasts.size()];
		int i = 0;
		for (Forecast forecast : forecasts) {
			forecastSeries[i] = new TimeSeries(forecast.getName(), Day.class);
			i++;
		}
		BigDecimal sum = BigDecimal.ZERO;
		Iterator<BigDecimal> iterator = inventoryValues.iterator();
		DateIterator dateIterator = intervalReport.dateIterator();
		for (BigDecimal v : values) {
			sum = sum.add(v);
			BigDecimal inventoryValue = iterator.next();
			de.tomsplayground.util.Day d = dateIterator.next();
			Day day = new Day(d.day, d.month+1, d.year);
			s1.add(day, sum.add(inventoryValue));
			int j = 0;
			for (Forecast forecast : forecasts) {
				if (! d.before(forecast.getStartDay()))
					forecastSeries[j].add(day, forecast.getValue(d));
				j++;
			}
		}
		TimeSeriesCollection dataset = new TimeSeriesCollection();
		dataset.addSeries(s1);
		for (TimeSeries timeSeries : forecastSeries) {
			dataset.addSeries(timeSeries);
		}		
		return dataset;
	}
	
	private TimeSeriesCollection createDeltaDataset(Interval interval, Class<? extends RegularTimePeriod> intervalClass) {
		Report report = getReport();
		TimeIntervalReport intervalReport = new TimeIntervalReport(report, interval, PriceProviderFactory.getInstance());
		List<BigDecimal> values = intervalReport.getValues();
		List<BigDecimal> inventoryValues = intervalReport.getInventoryValues();
		TimeSeries s1 = new TimeSeries(getEditorInput().getName(), intervalClass);
		BigDecimal lastInventoryValue = BigDecimal.ZERO;
		Iterator<BigDecimal> iterator = inventoryValues.iterator();
		DateIterator dateIterator = intervalReport.dateIterator();
		for (BigDecimal v : values) {
			BigDecimal inventoryValue = iterator.next();
			Calendar cal = dateIterator.next().toCalendar();
			RegularTimePeriod time = RegularTimePeriod.createInstance(intervalClass, cal.getTime(), cal.getTimeZone());
			s1.add(time, v.add(inventoryValue.subtract(lastInventoryValue)));
			lastInventoryValue = inventoryValue;
		}
		TimeSeriesCollection dataset = new TimeSeriesCollection();
		dataset.addSeries(s1);

		return dataset;
	}

	@Override
	public void setFocus() {
		// nothing to do
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		String type = displayType.getItem(displayType.getSelectionIndex());
		getReport().putConfigurationValue(CHART_TYPE, type);
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

}
