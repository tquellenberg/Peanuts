package de.tomsplayground.peanuts.client.editors.credit;

import java.awt.Color;
import java.text.SimpleDateFormat;

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
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Month;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.jfree.ui.RectangleInsets;

import de.tomsplayground.peanuts.client.chart.PeanutsDrawingSupplier;
import de.tomsplayground.peanuts.client.chart.TimeChart;
import de.tomsplayground.peanuts.domain.process.Credit;
import de.tomsplayground.peanuts.domain.process.ICredit;

public class CreditChartEditorPart extends EditorPart {

	private static final String CHART_TYPE = "creditChartType";

	private Combo displayType;
	private boolean dirty;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(input.getName());
		if (getCredit() == null) {
			throw new PartInitException("Invalid Input: Must be adaptable to " + Credit.class);
		}
	}

	protected Credit getCredit() {
		IEditorInput input = getEditorInput();
		return (Credit) input.getAdapter(Credit.class);
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite body = new Composite(parent, SWT.NONE);
		body.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		body.setLayout(new GridLayout());

		TimeSeriesCollection dataset = createTotalDataset();
		final JFreeChart chart = createChart(dataset);
		ChartComposite chartFrame = new ChartComposite(body, SWT.NONE, chart, true);
		chartFrame.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		final TimeChart timeChart = new TimeChart(chart, dataset);

		displayType = new Combo(body, SWT.READ_ONLY);
		displayType.add("all");
		displayType.add("one year");
		displayType.add("this year");
		displayType.add("6 month");
		displayType.add("1 month");
		String chartType = StringUtils.defaultString(getCredit().getConfigurationValue(CHART_TYPE), "all");
		displayType.setText(chartType);
		timeChart.setChartType(chartType);
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

	private JFreeChart createChart(IntervalXYDataset dataset) {
		JFreeChart chart;
		chart = ChartFactory.createTimeSeriesChart(
			getCredit().getName(), // title
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

		return chart;
	}

	private TimeSeriesCollection createTotalDataset() {
		TimeSeriesCollection dataset = new TimeSeriesCollection();
		ICredit credit = getCredit();
		Month month = new Month(credit.getStart().month + 1, credit.getStart().year);
		Month endMonth = new Month(credit.getEnd().month + 1, credit.getEnd().year);
		TimeSeries s1 = new TimeSeries(getCredit().getName(), Month.class);
		TimeSeries s2 = new TimeSeries(getCredit().getName(), Month.class);
		for (; month.compareTo(endMonth) <= 0; month = (Month)month.next()) {
			de.tomsplayground.util.Day day = new de.tomsplayground.util.Day(month.getYearValue(), month.getMonth() - 1, 1);
			s1.add(month, credit.amount(day));
			s2.add(month, credit.getInterest(day));
		}
		dataset.addSeries(s1);
		dataset.addSeries(s2);
		return dataset;
	}

	@Override
	public void setFocus() {
		// nothing to do
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		String type = displayType.getItem(displayType.getSelectionIndex());
		getCredit().putConfigurationValue(CHART_TYPE, type);
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
