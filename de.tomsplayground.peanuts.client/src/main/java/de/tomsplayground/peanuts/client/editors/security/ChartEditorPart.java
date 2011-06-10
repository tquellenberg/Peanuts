package de.tomsplayground.peanuts.client.editors.security;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Currency;
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
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYPointerAnnotation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.jfree.ui.LengthAdjustmentType;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.chart.PeanutsDrawingSupplier;
import de.tomsplayground.peanuts.client.chart.TimeChart;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.base.InventoryEntry;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.domain.statistics.Signal;
import de.tomsplayground.peanuts.domain.statistics.SimpleMovingAverage;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class ChartEditorPart extends EditorPart {

	private static final String CHART_TYPE = "chartType";
	boolean dirty = false;
	private Combo displayType;
	private TimeSeries priceTimeSeries;
	private IPriceProvider priceProvider;
	private ChartComposite chartComposite;
	
	private PropertyChangeListener priceProviderChangeListener = new PropertyChangeListener() {

		@Override
		public void propertyChange(final PropertyChangeEvent evt) {
			Display display = getSite().getWorkbenchWindow().getWorkbench().getDisplay();
			display.asyncExec(new Runnable() {				
				@Override
				public void run() {
					if (! chartComposite.isDisposed()) {
						if (evt.getOldValue() instanceof Price) {
							Price priceOld = (Price) evt.getOldValue();
							Day day = new Day(priceOld.getDay().day, priceOld.getDay().month+1, priceOld.getDay().year);
							priceTimeSeries.delete(day);
							if (isShowAvg()) {
								average20Days.delete(day);
								average100Days.delete(day);
							}
						}
						if (evt.getNewValue() instanceof Price) {
							Price priceNew = (Price) evt.getNewValue();
							de.tomsplayground.util.Day day = priceNew.getDay();
							priceTimeSeries.add(new Day(day.day, day.month+1, day.year), priceNew.getValue());
						} else  if (evt.getNewValue() != null) {
							// Full update
							for (Price p : priceProvider.getPrices()) {
								de.tomsplayground.util.Day day = p.getDay();
								priceTimeSeries.addOrUpdate(new Day(day.day, day.month+1, day.year), p.getValue());
							}					
						}
						if (isShowAvg()) {
							createMovingAverage(average20Days, 20);
							createMovingAverage(average100Days, 100);
						}
					}
				}
			});
		}
	};
	
	private PropertyChangeListener securityPropertyChangeListener = new PropertyChangeListener() {	
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getPropertyName().equals("SHOW_AVG")) {
				if (isShowAvg()) {
					createMovingAverage(average20Days, 20);
					createMovingAverage(average100Days, 100);
					dataset.addSeries(average20Days);
					dataset.addSeries(average100Days);
				} else {
					dataset.removeSeries(average20Days);
					dataset.removeSeries(average100Days);
				}				
			}
			if (evt.getPropertyName().equals("SHOW_SIGNALS")) {
				if (isShowSignals()) {
					signalAnnotations = timeChart.addSignals(createSignals());
				} else {
					timeChart.removeAnnotations(signalAnnotations);
				}
			}
			if (evt.getPropertyName().equals("STOPLOSS")) {
				updateStopLossMarker();
			}
		}
	};
	
	private TimeSeries average20Days;
	private TimeSeries average100Days;
	private TimeSeriesCollection dataset;
	private ImmutableList<XYAnnotation> signalAnnotations;
	private TimeChart timeChart;
	private ValueMarker stopLossMarker;
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if ( !(input instanceof SecurityEditorInput)) {
			throw new PartInitException("Invalid Input: Must be SecurityEditorInput");
		}
		setSite(site);
		setInput(input);
		setPartName(input.getName());
		Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();
		priceProvider = PriceProviderFactory.getInstance().getPriceProvider(security);
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite body = new Composite(parent, SWT.NONE);
		body.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		body.setLayout(new GridLayout());

		createDataset();
		final JFreeChart chart = createChart();
		chartComposite = new ChartComposite(body, SWT.NONE, chart, true);
		chartComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		timeChart = new TimeChart(chart, dataset);
		if (isShowSignals()) {
			signalAnnotations = timeChart.addSignals(createSignals());
		}
		
		addOrderAnnotations(chart);

		updateStopLossMarker();

		Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();
		
		BigDecimal avgPrice = getAvgPrice();
		if (avgPrice != null) {
			ValueMarker marker = new ValueMarker(avgPrice.doubleValue());
			marker.setPaint(Color.black);
			marker.setLabelPaint(Color.black);
			marker.setLabel("Avg Price");
			marker.setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
			marker.setLabelOffsetType(LengthAdjustmentType.EXPAND);
			marker.setLabelAnchor(RectangleAnchor.BOTTOM_RIGHT);
			marker.setLabelTextAnchor(TextAnchor.TOP_RIGHT);

			((XYPlot)chart.getPlot()).addRangeMarker(marker);
		}
		
		displayType = new Combo(body, SWT.READ_ONLY);
		displayType.add("all");
		displayType.add("three years");
		displayType.add("one year");
		displayType.add("this year");
		displayType.add("6 month");
		displayType.add("1 month");
		String chartType = StringUtils.defaultString(security.getConfigurationValue(CHART_TYPE), "all");
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
		
		security.addPropertyChangeListener(securityPropertyChangeListener);
	}

	protected void updateStopLossMarker() {
		Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();
		String stopLossValue = security.getConfigurationValue("STOPLOSS");
		if (stopLossMarker != null) {
			timeChart.getPlot().removeRangeMarker(stopLossMarker);
			stopLossMarker = null;
		}
		if (StringUtils.isNotEmpty(stopLossValue)) {
			try {
				stopLossMarker = new ValueMarker(PeanutsUtil.parseQuantity(stopLossValue).doubleValue());
				stopLossMarker.setPaint(Color.red);
				stopLossMarker.setLabelPaint(Color.red);
				stopLossMarker.setLabel("Stop Loss");
				stopLossMarker.setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
				stopLossMarker.setLabelOffsetType(LengthAdjustmentType.EXPAND);
				stopLossMarker.setLabelAnchor(RectangleAnchor.BOTTOM_RIGHT);
				stopLossMarker.setLabelTextAnchor(TextAnchor.TOP_RIGHT);

				timeChart.getPlot().addRangeMarker(stopLossMarker);
			} catch (ParseException e) {
				// Okay
			}
		}
	}

	protected void addOrderAnnotations(final JFreeChart chart) {
		ImmutableList<InvestmentTransaction> transactions = getOrders();
		for (InvestmentTransaction investmentTransaction : transactions) {
			de.tomsplayground.util.Day day = investmentTransaction.getDay();
			long x = new Day(day.day, day.month+1, day.year).getFirstMillisecond();
			double y = priceProvider.getPrice(day).getClose().doubleValue();
			
			XYPointerAnnotation pointerAnnotation = null;
			String t = "";
			Color c = Color.BLACK;
			switch (investmentTransaction.getType()) {
			case BUY:
				t = "+"+investmentTransaction.getQuantity();
				pointerAnnotation = new XYPointerAnnotation(t, x, y, Math.PI / 2);
				c = Color.GREEN;
				break;
			case SELL:
				t = "-"+investmentTransaction.getQuantity();
				pointerAnnotation = new XYPointerAnnotation(t, x, y, 3* Math.PI / 2);
				c = Color.RED;
				break;
			case INCOME:
				t = " +"+PeanutsUtil.formatCurrency(investmentTransaction.getAmount(), Currency.getInstance("EUR"));
				pointerAnnotation = new XYPointerAnnotation(t, x, y, Math.PI / 2);
				c = Color.GREEN;
				break;
			case EXPENSE:
				t = " -"+PeanutsUtil.formatCurrency(investmentTransaction.getAmount(), Currency.getInstance("EUR"));
				pointerAnnotation = new XYPointerAnnotation(t, x, y, 3* Math.PI / 2);
				c = Color.RED;
				break;
			}
			if (pointerAnnotation != null) {
				pointerAnnotation.setPaint(c);
				pointerAnnotation.setArrowPaint(c);
				pointerAnnotation.setToolTipText(PeanutsUtil.formatDate(day) + " " + t);
				pointerAnnotation.setArrowWidth(5);
				((XYPlot)chart.getPlot()).addAnnotation(pointerAnnotation);
			}
		}
	}
	
	private ImmutableList<InvestmentTransaction> getOrders() {
		Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();
		Inventory inventory = Activator.getDefault().getAccountManager().getFullInventory();
		InventoryEntry inventoryEntry = inventory.getEntry(security);
		if (inventoryEntry != null)
			return inventoryEntry.getTransactions();
		else
			return ImmutableList.of();
	}
	
	private BigDecimal getAvgPrice() {
		Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();		
		Inventory inventory = Activator.getDefault().getAccountManager().getFullInventory();
		if (inventory.getSecurities().contains(security)) {
			return inventory.getEntry(security).getAvgPrice();
		}
		return null;
	}

	/**
	 * Creates a chart.
	 *
	 * @param dataset a dataset.
	 *
	 * @return A chart.
	 */
	private JFreeChart createChart() {

		JFreeChart chart = ChartFactory.createTimeSeriesChart(getEditorInput().getName(), // title
			"Date", // x-axis label
			"Price", // y-axis label
			dataset, // data
			true, // create legend?
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
		XYItemRenderer renderer = plot.getRenderer();
		if (renderer instanceof XYLineAndShapeRenderer) {
			renderer.setSeriesStroke(1, new BasicStroke(2.0f));
			renderer.setSeriesStroke(2, new BasicStroke(2.0f));
		}
		
		DateAxis axis = (DateAxis) plot.getDomainAxis();
		axis.setDateFormatOverride(new SimpleDateFormat("MMM-yyyy"));

		return chart;
	}

	private void createDataset() {
		priceTimeSeries = new TimeSeries(getEditorInput().getName(), Day.class);
		for (Price price : priceProvider.getPrices()) {
			de.tomsplayground.util.Day day = price.getDay();
			priceTimeSeries.add(new Day(day.day, day.month+1, day.year), price.getValue());
		}
		dataset = new TimeSeriesCollection();
		dataset.addSeries(priceTimeSeries);

		average20Days = new TimeSeries("Moving Average: 20 days", Day.class);
		average100Days = new TimeSeries("Moving Average: 100 days", Day.class);

		if (isShowAvg()) {
			createMovingAverage(average20Days, 20);
			dataset.addSeries(average20Days);			
			createMovingAverage(average100Days, 100);
			dataset.addSeries(average100Days);
		}
		
		((ObservableModelObject) priceProvider).addPropertyChangeListener(priceProviderChangeListener);
	}

	private boolean isShowAvg() {
		return Boolean.parseBoolean(((SecurityEditorInput)getEditorInput()).getSecurity().getConfigurationValue("SHOW_AVG"));
	}
	
	private boolean isShowSignals() {
		return Boolean.parseBoolean(((SecurityEditorInput)getEditorInput()).getSecurity().getConfigurationValue("SHOW_SIGNALS"));
	}

	@Override
	public void dispose() {
		((ObservableModelObject) priceProvider).removePropertyChangeListener(priceProviderChangeListener);
		((SecurityEditorInput)getEditorInput()).getSecurity().removePropertyChangeListener(securityPropertyChangeListener);
		super.dispose();
	}
	
	private void createMovingAverage(TimeSeries a1, int days) {
		SimpleMovingAverage simpleMovingAverage = new SimpleMovingAverage(days);
		List<Price> sma = simpleMovingAverage.calculate(priceProvider.getPrices());
		for (Price price : sma) {
			de.tomsplayground.util.Day day = price.getDay();
			a1.addOrUpdate(new Day(day.day, day.month+1, day.year), price.getValue());
		}
	}

	private ImmutableList<Signal> createSignals() {
		SimpleMovingAverage simpleMovingAverage = new SimpleMovingAverage(20);
		simpleMovingAverage.calculate(priceProvider.getPrices());
		return simpleMovingAverage.getSignals();
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
	public void doSave(IProgressMonitor monitor) {
		Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();
		security.putConfigurationValue(CHART_TYPE, displayType.getItem(displayType.getSelectionIndex()));
		dirty = false;
	}

	@Override
	public void doSaveAs() {
		// nothing to do
	}

	@Override
	public void setFocus() {
		// nothing to do
	}

}
