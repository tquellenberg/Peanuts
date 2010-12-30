package de.tomsplayground.peanuts.client.editors.account;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.jfree.ui.RectangleInsets;

import de.tomsplayground.peanuts.client.chart.PeanutsDrawingSupplier;
import de.tomsplayground.peanuts.client.chart.TimeChart;
import de.tomsplayground.peanuts.client.util.UniqueAsyncExecution;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.domain.reporting.transaction.DateIterator;
import de.tomsplayground.peanuts.domain.reporting.transaction.TimeIntervalReport;

public class ValueChartEditorPart extends EditorPart {
	
	private static final String CHART_TYPE = "chartType";

	private Combo displayType;
	private boolean dirty;
	private TimeChart timeChart;

	private TimeIntervalReport intervalReport;

	private ChartComposite chartFrame;

	private PropertyChangeListener changeListener = new UniqueAsyncExecution() {
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

		Account account = ((AccountEditorInput)getEditorInput()).getAccount();
		intervalReport = new TimeIntervalReport(account, TimeIntervalReport.Interval.DAY, PriceProviderFactory.getInstance());
		intervalReport.addPropertyChangeListener(changeListener);

		Map<String, String> displayConfiguration = ((AccountEditorInput) getEditorInput()).getAccount().getDisplayConfiguration();
		String chartType = "all";
		if (displayConfiguration.containsKey(CHART_TYPE))
			chartType = displayConfiguration.get(CHART_TYPE);

		JFreeChart chart = createChart(chartType);
		chartFrame = new ChartComposite(body, SWT.NONE, chart, true);
		chartFrame.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		displayType = new Combo(body, SWT.READ_ONLY);
		displayType.add("all");
		displayType.add("three years");
		displayType.add("one year");
		displayType.add("this year");
		displayType.add("6 month");
		displayType.add("1 month");
		displayType.setText(chartType);
		displayType.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				Combo c = (Combo)e.getSource();
				String type = c.getItem(c.getSelectionIndex());
				timeChart.setChartType(type);
				dirty = true;
				firePropertyChange(IEditorPart.PROP_DIRTY);
			}
		});		
	}
	
	@Override
	public void dispose() {
		intervalReport.removePropertyChangeListener(changeListener);
		super.dispose();
	}

	private JFreeChart createChart(String chartType) {
		TimeSeriesCollection dataset = createTotalDataset();
		JFreeChart chart;
			chart = ChartFactory.createTimeSeriesChart(
					getEditorInput().getName(), // title
					"Date", // x-axis label
					"Value", // y-axis label
					dataset, // data
					false, // create legend?
					true, // generate tooltips?
					false // generate URLs?
				);		
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
		axis.setDateFormatOverride(new SimpleDateFormat("MMM-yyyy"));

		timeChart = new TimeChart(chart, dataset);
		timeChart.setChartType(chartType);
		
		return chart;
	}

	private TimeSeriesCollection createTotalDataset() {
		List<BigDecimal> values = intervalReport.getValues();
		DateIterator dateIterator = intervalReport.dateIterator();
		List<BigDecimal> inventoryValues = intervalReport.getInventoryValues();
		List<BigDecimal> investmentValues = intervalReport.getInvestmentValues();
		TimeSeries s1 = new TimeSeries(getEditorInput().getName(), Day.class);
		TimeSeries s2 = new TimeSeries("Invested sum", Day.class);
		BigDecimal sum = BigDecimal.ZERO;
		Iterator<BigDecimal> iterator1 = inventoryValues.iterator();
		Iterator<BigDecimal> iterator2 = investmentValues.iterator();
		for (BigDecimal v : values) {
			sum = sum.add(v);
			de.tomsplayground.util.Day d = dateIterator.next();
			Day day = new Day(d.getDay(), d.getMonth()+1, d.getYear());
			s1.add(day, sum.add(iterator1.next()));
			s2.add(day, iterator2.next());
		}
		TimeSeriesCollection dataset = new TimeSeriesCollection();
		dataset.addSeries(s1);
		dataset.addSeries(s2);

		return dataset;
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		Map<String, String> displayConfiguration = ((AccountEditorInput)getEditorInput()).getAccount().getDisplayConfiguration();
		String type = displayType.getItem(displayType.getSelectionIndex());
		displayConfiguration.put(CHART_TYPE, type);
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
		// nothing to do
	}

}
