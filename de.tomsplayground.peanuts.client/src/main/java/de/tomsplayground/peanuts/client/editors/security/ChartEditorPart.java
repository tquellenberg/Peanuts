package de.tomsplayground.peanuts.client.editors.security;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYPointerAnnotation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.data.Range;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.jfree.ui.LengthAdjustmentType;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.chart.PeanutsDrawingSupplier;
import de.tomsplayground.peanuts.client.chart.TimeChart;
import de.tomsplayground.peanuts.client.editors.security.properties.ChartPropertyPage;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.base.InventoryEntry;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.domain.currenncy.Currencies;
import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;
import de.tomsplayground.peanuts.domain.currenncy.ExchangeRates;
import de.tomsplayground.peanuts.domain.fundamental.AvgFundamentalData;
import de.tomsplayground.peanuts.domain.fundamental.FundamentalDatas;
import de.tomsplayground.peanuts.domain.process.CurrencyAdjustedPriceProvider;
import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.domain.process.StockSplit;
import de.tomsplayground.peanuts.domain.process.StopLoss;
import de.tomsplayground.peanuts.domain.statistics.SimpleMovingAverage;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class ChartEditorPart extends EditorPart {

	private static final Color FAIR_PRICE_COLOR = new Color(154, 205, 50);
	private static final BigDecimal HUNDRED = new BigDecimal(100);
	private static final MathContext MC = new MathContext(10, RoundingMode.HALF_EVEN);
	private static final String CHART_TYPE = "chartType";

	private static final Range peRatioChartRange = new Range(-35.0, 35.0);

	boolean dirty = false;
	private Combo displayType;
	private TimeSeries priceTimeSeries;
	private IPriceProvider priceProvider;
	private ChartComposite chartComposite;

	private final PropertyChangeListener priceProviderChangeListener = new PropertyChangeListener() {

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
							for (IPrice p : priceProvider.getPrices()) {
								de.tomsplayground.util.Day day = p.getDay();
								priceTimeSeries.addOrUpdate(new Day(day.day, day.month+1, day.year), p.getValue());
							}
						}
						if (isShowAvg()) {
							createMovingAverage(average20Days, 20);
							createMovingAverage(average100Days, 100);
						}
						updateStopLoss();
					}
				}
			});
		}
	};

	private final PropertyChangeListener securityPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (chartComposite.isDisposed()) {
				return;
			}
			if (evt.getPropertyName().equals(ChartPropertyPage.CONF_SHOW_AVG)) {
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
			if (evt.getPropertyName().equals(ChartPropertyPage.CONF_SHOW_BUY_SELL) ||
				evt.getPropertyName().equals(ChartPropertyPage.CONF_SHOW_DIVIDENDS)) {
				timeChart.removeAnnotations(orderAnnotations);
				orderAnnotations = addOrderAnnotations();
				if (avgPriceAnnotation != null) {
					pricePlot.removeRangeMarker(avgPriceAnnotation);
				}
				addAvgPriceAnnotation();
			}
			if (evt.getPropertyName().equals(FundamentalDatas.OVERRIDDEN_AVG_PE) ||
				evt.getPropertyName().equals("fundamentalData")) {
				calculateFixedPePrice();
			}
		}
	};

	private final PropertyChangeListener accountManagerChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getPropertyName().equals("stopLoss")) {
				updateStopLoss();
			}
		}
	};

	private TimeSeries average20Days;
	private TimeSeries average100Days;
	// Upper chart
	private final TimeSeriesCollection dataset = new TimeSeriesCollection();
	// Lower chart
	private final TimeSeriesCollection dataset2 = new TimeSeriesCollection();
	private ImmutableList<XYAnnotation> orderAnnotations = ImmutableList.of();
	private TimeChart timeChart;
	private TimeSeries stopLoss;
	private TimeSeries compareToPriceTimeSeries;
	private TimeSeries fixedPePrice;
	private XYPlot pricePlot;
	private Button convertToEuro;
	private Button convertToUSD;
	private ValueMarker avgPriceAnnotation;
	private TimeSeries peDeltaTimeSeries;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if ( !(input instanceof SecurityEditorInput)) {
			throw new PartInitException("Invalid Input: Must be SecurityEditorInput");
		}
		setSite(site);
		setInput(input);
		setPartName(input.getName());
		Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();
		ImmutableList<StockSplit> stockSplits = Activator.getDefault().getAccountManager().getStockSplits(security);
		priceProvider = PriceProviderFactory.getInstance().getAdjustedPriceProvider(security, stockSplits);
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite body = new Composite(parent, SWT.NONE);
		body.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		body.setLayout(layout);

		createDataset();
		final JFreeChart chart = createChart();
		chartComposite = new ChartComposite(body, SWT.NONE, chart, true);
		chartComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		timeChart = new TimeChart(chart, dataset);

		orderAnnotations = addOrderAnnotations();

		Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();

		addAvgPriceAnnotation();

		addSplitAnnotations(Activator.getDefault().getAccountManager().getStockSplits(security));

		Composite buttons = new Composite(body, SWT.NONE);
		buttons.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		GridLayout layout2 = new GridLayout(3, false);
		layout2.marginHeight = 5;
		layout2.marginWidth = 10;
		layout2.verticalSpacing = 0;
		buttons.setLayout(layout2);
		buttons.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

		displayType = new Combo(buttons, SWT.READ_ONLY);
		for (TimeChart.RANGE r : TimeChart.RANGE.values()) {
			displayType.add(r.getName());
		}
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
				calculateCompareToValues();
			}
		});
		Currency defaultCurrency = Currencies.getInstance().getDefaultCurrency();
		if (security.getCurrency() != null && ! security.getCurrency().equals(defaultCurrency)) {
			Label text = new Label(buttons, SWT.NONE);
			text.setText("Convert from "+security.getCurrency().getCurrencyCode()+" to "+defaultCurrency.getCurrencyCode());
			convertToEuro = new Button(buttons, SWT.CHECK);
			convertToEuro.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					createDataset();
					pricePlot.clearAnnotations();
					addOrderAnnotations();
					pricePlot.getRangeAxis().setLabel("Price "+getChartCurrencyConverter().getToCurrency().getSymbol());
					timeChart.setChartType(displayType.getItem(displayType.getSelectionIndex()));
				}
			});
		} else {
			Label text = new Label(buttons, SWT.NONE);
			text.setText("Convert from "+defaultCurrency.getCurrencyCode()+" to USD");
			convertToUSD = new Button(buttons, SWT.CHECK);
			convertToUSD.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					createDataset();
					pricePlot.clearAnnotations();
					addOrderAnnotations();
					pricePlot.getRangeAxis().setLabel("Price "+getChartCurrencyConverter().getToCurrency().getSymbol());
					timeChart.setChartType(displayType.getItem(displayType.getSelectionIndex()));
				}
			});
		}

		calculateCompareToValues();

		security.addPropertyChangeListener(securityPropertyChangeListener);

		Activator.getDefault().getAccountManager().addPropertyChangeListener(accountManagerChangeListener);
	}

	private void addAvgPriceAnnotation() {
		BigDecimal avgPrice = getAvgPrice();
		if (avgPrice != null && isShowBuyAndSell()) {
			avgPriceAnnotation = new ValueMarker(avgPrice.doubleValue());
			avgPriceAnnotation.setPaint(Color.black);
			avgPriceAnnotation.setLabelPaint(Color.black);
			avgPriceAnnotation.setLabel("Avg Price");
			avgPriceAnnotation.setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
			avgPriceAnnotation.setLabelOffsetType(LengthAdjustmentType.EXPAND);
			avgPriceAnnotation.setLabelAnchor(RectangleAnchor.BOTTOM_RIGHT);
			avgPriceAnnotation.setLabelTextAnchor(TextAnchor.TOP_RIGHT);

			pricePlot.addRangeMarker(avgPriceAnnotation);
		} else {
			avgPriceAnnotation = null;
		}
	}

	private IPriceProvider getChartPriceProvider() {
		if (convertToEuro != null && convertToEuro.getSelection()) {
			Currency defaultCurrency = Currencies.getInstance().getDefaultCurrency();
			Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();
			ExchangeRates exchangeRate = Activator.getDefault().getExchangeRate();
			CurrencyConverter currencyConverter = exchangeRate.createCurrencyConverter(security.getCurrency(), defaultCurrency);
			if (currencyConverter != null) {
				return new CurrencyAdjustedPriceProvider(priceProvider, currencyConverter);
			}
		}
		if (convertToUSD != null && convertToUSD.getSelection()) {
			Currency defaultCurrency = Currencies.getInstance().getDefaultCurrency();
			Currency usdCurrency = Currency.getInstance("USD");
			ExchangeRates exchangeRate = Activator.getDefault().getExchangeRate();
			CurrencyConverter currencyConverter = exchangeRate.createCurrencyConverter(defaultCurrency, usdCurrency);
			if (currencyConverter != null) {
				return new CurrencyAdjustedPriceProvider(priceProvider, currencyConverter);
			}
		}
		return priceProvider;
	}

	private CurrencyConverter getChartCurrencyConverter() {
		Currency defaultCurrency = Currencies.getInstance().getDefaultCurrency();
		Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();
		ExchangeRates exchangeRate = Activator.getDefault().getExchangeRate();
		if (convertToEuro != null && convertToEuro.getSelection()) {
			CurrencyConverter currencyConverter = exchangeRate.createCurrencyConverter(security.getCurrency(), defaultCurrency);
			if (currencyConverter != null) {
				return currencyConverter;
			}
		}
		if (convertToUSD != null && convertToUSD.getSelection()) {
			Currency usdCurrency = Currency.getInstance("USD");
			CurrencyConverter currencyConverter = exchangeRate.createCurrencyConverter(defaultCurrency, usdCurrency);
			if (currencyConverter != null) {
				return currencyConverter;
			}
		}
		return exchangeRate.createCurrencyConverter(security.getCurrency(), security.getCurrency());
	}

	protected void addSplitAnnotations(List<StockSplit> splits) {
		for (StockSplit stockSplit : splits) {
			de.tomsplayground.util.Day day = stockSplit.getDay();
			long x = new Day(day.day, day.month+1, day.year).getFirstMillisecond();
			ValueMarker valueMarker = new ValueMarker(x);
			valueMarker.setLabelTextAnchor(TextAnchor.TOP_RIGHT);
			valueMarker.setLabel("Split "+stockSplit.getFrom()+":"+stockSplit.getTo());
			pricePlot.addDomainMarker(valueMarker);
		}
	}

	private BigDecimal getSplitRatio(de.tomsplayground.util.Day day) {
		Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();
		ImmutableList<StockSplit> stockSplits = Activator.getDefault().getAccountManager().getStockSplits(security);
		return stockSplits.stream()
			.filter(sp -> day.before(sp.getDay()))
			.map(sp -> sp.getRatio())
			.reduce(BigDecimal.ONE, BigDecimal::multiply);
	}

	protected ImmutableList<XYAnnotation> addOrderAnnotations() {
		List<XYAnnotation> annotations = new ArrayList<>();
		IPriceProvider pp = getChartPriceProvider();
		for (InvestmentTransaction investmentTransaction : getOrders()) {
			de.tomsplayground.util.Day day = investmentTransaction.getDay();
			long x = new Day(day.day, day.month+1, day.year).getFirstMillisecond();
			double y = pp.getPrice(day).getClose().doubleValue();

			XYPointerAnnotation pointerAnnotation = null;
			String t = "";
			Color c = Color.BLACK;
			org.eclipse.swt.graphics.Color swtColor;
			switch (investmentTransaction.getType()) {
				case BUY:
					if (!isShowBuyAndSell()) {
						break;
					}
					t = "+"+investmentTransaction.getQuantity();
					BigDecimal adjustedPrice = investmentTransaction.getPrice().multiply(getSplitRatio(day));
					adjustedPrice = getChartCurrencyConverter().convert(adjustedPrice, day);
					pointerAnnotation = new XYPointerAnnotation(t, x, adjustedPrice.doubleValue(), Math.PI / 2);
					swtColor = Activator.getDefault().getColorProvider().get(Activator.GREEN);
					c = new Color(swtColor.getRed(), swtColor.getGreen(), swtColor.getBlue());
					break;
				case SELL:
					if (!isShowBuyAndSell()) {
						break;
					}
					t = "-"+investmentTransaction.getQuantity();
					adjustedPrice = investmentTransaction.getPrice().multiply(getSplitRatio(day));
					adjustedPrice = getChartCurrencyConverter().convert(adjustedPrice, day);
					pointerAnnotation = new XYPointerAnnotation(t, x, adjustedPrice.doubleValue(), 3* Math.PI / 2);
					swtColor = Activator.getDefault().getColorProvider().get(Activator.RED);
					c = new Color(swtColor.getRed(), swtColor.getGreen(), swtColor.getBlue());
					break;
				case INCOME:
					if (!isShowDividends()) {
						break;
					}
					t = " +"+PeanutsUtil.formatCurrency(investmentTransaction.getAmount(), Currency.getInstance("EUR"));
					pointerAnnotation = new XYPointerAnnotation(t, x, y, Math.PI / 2);
					break;
				case EXPENSE:
					if (!isShowDividends()) {
						break;
					}
					t = " -"+PeanutsUtil.formatCurrency(investmentTransaction.getAmount(), Currency.getInstance("EUR"));
					pointerAnnotation = new XYPointerAnnotation(t, x, y, 3* Math.PI / 2);
					break;
			}
			if (pointerAnnotation != null) {
				pointerAnnotation.setPaint(c);
				pointerAnnotation.setArrowPaint(c);
				pointerAnnotation.setToolTipText(PeanutsUtil.formatDate(day) + " " + t);
				pointerAnnotation.setArrowWidth(5);
				pointerAnnotation.setLabelOffset(5);
				pricePlot.addAnnotation(pointerAnnotation);
				annotations.add(pointerAnnotation);
			}
		}
		return ImmutableList.copyOf(annotations);
	}

	private ImmutableList<InvestmentTransaction> getOrders() {
		Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();
		Inventory inventory = Activator.getDefault().getAccountManager().getFullInventory();
		InventoryEntry inventoryEntry = inventory.getEntry(security);
		if (inventoryEntry != null) {
			return inventoryEntry.getTransactions();
		} else {
			return ImmutableList.of();
		}
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
		Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();
		FundamentalDatas fundamentalDatas = security.getFundamentalDatas();
		boolean showPeDeltaChart = ! fundamentalDatas.isEmpty();

		DateAxis axis = new DateAxis("Date");
		axis.setDateFormatOverride(new SimpleDateFormat("MMM-yyyy"));
		CombinedDomainXYPlot combiPlot = new CombinedDomainXYPlot(axis);
		combiPlot.setDrawingSupplier(new PeanutsDrawingSupplier());
		JFreeChart chart = new JFreeChart(getEditorInput().getName(), combiPlot);
		chart.setBackgroundPaint(Color.WHITE);

		StandardXYItemRenderer renderer = new StandardXYItemRenderer();
		renderer.setBaseToolTipGenerator(StandardXYToolTipGenerator.getTimeSeriesInstance());
		renderer.setSeriesPaint(0, Color.BLACK);
		int nextPos = 1;
		if (isShowAvg()) {
			renderer.setSeriesStroke(nextPos++, new BasicStroke(2.0f));
			renderer.setSeriesStroke(nextPos++, new BasicStroke(2.0f));
		}
		// Stop loss
		renderer.setSeriesPaint(nextPos++, Color.GREEN);
		if (getCompareTo() != null) {
			// Compare to
			renderer.setSeriesPaint(nextPos++, Color.LIGHT_GRAY);
		}
		renderer.setSeriesPaint(nextPos++, FAIR_PRICE_COLOR);

		NumberAxis rangeAxis2 = new NumberAxis("Price "+getChartCurrencyConverter().getToCurrency().getSymbol());
		rangeAxis2.setAutoRange(false);
		pricePlot = new XYPlot(dataset, null, rangeAxis2, renderer);
		combiPlot.add(pricePlot, showPeDeltaChart ? 70 : 100);
		pricePlot.setBackgroundPaint(PeanutsDrawingSupplier.BACKGROUND_PAINT);
		pricePlot.setDomainGridlinePaint(PeanutsDrawingSupplier.GRIDLINE_PAINT);
		pricePlot.setRangeGridlinePaint(PeanutsDrawingSupplier.GRIDLINE_PAINT);
		pricePlot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
		pricePlot.setDomainCrosshairVisible(true);
		pricePlot.setRangeCrosshairVisible(true);

		if (showPeDeltaChart) {
			XYAreaRenderer xyAreaRenderer = new XYAreaRenderer();
			xyAreaRenderer.setBaseToolTipGenerator(StandardXYToolTipGenerator.getTimeSeriesInstance());
			xyAreaRenderer.setSeriesPaint(0, new PeanutsDrawingSupplier().getNextPaint());
			NumberAxis rangeAxis = new NumberAxis("PE delta %");
			rangeAxis.setAutoRange(false);
			rangeAxis.setRange(peRatioChartRange);

			XYPlot plot2 = new XYPlot(dataset2, null, rangeAxis, xyAreaRenderer);
			plot2.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
			plot2.setDomainCrosshairVisible(true);
			plot2.setRangeCrosshairVisible(true);
			combiPlot.add(plot2, 30);
		}
		return chart;
	}

	private void calculateFixedPePrice() {
		Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();
		FundamentalDatas fundamentalDatas = security.getFundamentalDatas();
		if (fundamentalDatas.isEmpty()) {
			return;
		}
		ExchangeRates exchangeRate = Activator.getDefault().getExchangeRate();
		BigDecimal avgPE = fundamentalDatas.getOverriddenAvgPE();
		if (avgPE == null) {
			AvgFundamentalData avgData = fundamentalDatas.getAvgFundamentalData(priceProvider, exchangeRate);
			avgPE = avgData.getAvgPE();
		}
		for (IPrice price : priceProvider.getPrices()) {
			de.tomsplayground.util.Day day = price.getDay();
			BigDecimal pe = fundamentalDatas.getAdjustedContinuousEarnings(day, exchangeRate);
			if (pe != null && pe.signum() > 0) {
				BigDecimal fairPrice = pe.multiply(avgPE, MC);
				// Main Chart
				fixedPePrice.addOrUpdate(new Day(day.day, day.month+1, day.year), fairPrice);
				if (fairPrice.signum() == 1) {
					// Delta Chart
					BigDecimal deltaPercent = price.getClose().subtract(fairPrice).multiply(HUNDRED).divide(fairPrice, MC);
					peDeltaTimeSeries.addOrUpdate(new Day(day.day, day.month+1, day.year), deltaPercent.doubleValue());
				}
			}
		}
	}

	private void createDataset() {
		dataset.removeAllSeries();
		dataset2.removeAllSeries();

		priceTimeSeries = new TimeSeries(getEditorInput().getName(), Day.class);
		for (IPrice price : getChartPriceProvider().getPrices()) {
			de.tomsplayground.util.Day day = price.getDay();
			priceTimeSeries.add(new Day(day.day, day.month+1, day.year), price.getValue());
		}
		dataset.addSeries(priceTimeSeries);

		average20Days = new TimeSeries("MA20", Day.class);
		average100Days = new TimeSeries("MA100", Day.class);

		if (isShowAvg()) {
			createMovingAverage(average20Days, 20);
			dataset.addSeries(average20Days);
			createMovingAverage(average100Days, 100);
			dataset.addSeries(average100Days);
		}

		stopLoss = new TimeSeries("Stop Loss", Day.class);
		updateStopLoss();
		dataset.addSeries(stopLoss);

		Security compareTo = getCompareTo();
		if (compareTo != null) {
			compareToPriceTimeSeries = new TimeSeries(compareTo.getName(), Day.class);
			dataset.addSeries(compareToPriceTimeSeries);
		}

		fixedPePrice = new TimeSeries("EPS * avg PE", Day.class);
		peDeltaTimeSeries = new TimeSeries("PE delta %", Day.class);
		calculateFixedPePrice();
		if (! fixedPePrice.isEmpty()) {
			dataset.addSeries(fixedPePrice);
			dataset2.addSeries(peDeltaTimeSeries);
		}

		if (priceProvider instanceof ObservableModelObject) {
			((ObservableModelObject) priceProvider).addPropertyChangeListener(priceProviderChangeListener);
		}
	}

	private void calculateCompareToValues() {
		Security compareTo = getCompareTo();
		if (compareTo != null && timeChart.getFromDate() != null) {
			de.tomsplayground.util.Day fromDate = de.tomsplayground.util.Day.fromCalendar(timeChart.getFromDate());
			ImmutableList<StockSplit> stockSplits = Activator.getDefault().getAccountManager().getStockSplits(compareTo);
			IPriceProvider compareToPriceProvider = PriceProviderFactory.getInstance().getAdjustedPriceProvider(compareTo, stockSplits);
			IPrice p1 = priceProvider.getPrice(fromDate);
			IPrice p2 = compareToPriceProvider.getPrice(fromDate);
			BigDecimal adjust = p1.getValue().divide(p2.getValue(), MC);
			for (IPrice price : compareToPriceProvider.getPrices()) {
				de.tomsplayground.util.Day day = price.getDay();
				BigDecimal value = price.getValue().multiply(adjust);
				compareToPriceTimeSeries.addOrUpdate(new Day(day.day, day.month+1, day.year), value);
			}
		}
	}

	private void updateStopLoss() {
		Security security = ((SecurityEditorInput)getEditorInput()).getSecurity();
		ImmutableSet<StopLoss> stopLosses = Activator.getDefault().getAccountManager().getStopLosses(security);
		if (! stopLosses.isEmpty()) {
			createStopLoss(stopLoss, stopLosses.iterator().next());
		}
	}

	private boolean isShowAvg() {
		return Boolean.parseBoolean(((SecurityEditorInput)getEditorInput()).getSecurity()
			.getConfigurationValue(ChartPropertyPage.CONF_SHOW_AVG));
	}

	private boolean isShowBuyAndSell() {
		return Boolean.parseBoolean(Objects.toString(((SecurityEditorInput)getEditorInput()).getSecurity()
			.getConfigurationValue(ChartPropertyPage.CONF_SHOW_BUY_SELL), "true"));
	}

	private boolean isShowDividends() {
		return Boolean.parseBoolean(Objects.toString(((SecurityEditorInput)getEditorInput()).getSecurity()
			.getConfigurationValue(ChartPropertyPage.CONF_SHOW_DIVIDENDS), "true"));
	}

	private Security getCompareTo() {
		final String isin = ((SecurityEditorInput)getEditorInput()).getSecurity()
			.getConfigurationValue(ChartPropertyPage.CONF_COMPARE_WITH);
		if (StringUtils.isNoneEmpty(isin)) {
			ImmutableList<Security> securities = Activator.getDefault().getAccountManager().getSecurities();
			return Iterables.find(securities, new Predicate<Security>() {
				@Override
				public boolean apply(Security arg0) {
					return StringUtils.equals(arg0.getISIN(), isin);
				}
			}, null);
		}
		return null;
	}

	@Override
	public void dispose() {
		if (priceProvider instanceof ObservableModelObject) {
			((ObservableModelObject) priceProvider).removePropertyChangeListener(priceProviderChangeListener);
		}
		((SecurityEditorInput)getEditorInput()).getSecurity().removePropertyChangeListener(securityPropertyChangeListener);
		Activator.getDefault().getAccountManager().removePropertyChangeListener(accountManagerChangeListener);
		super.dispose();
	}

	private void createMovingAverage(TimeSeries a1, int days) {
		SimpleMovingAverage simpleMovingAverage = new SimpleMovingAverage(days);
		List<IPrice> sma = simpleMovingAverage.calculate(priceProvider.getPrices());
		for (IPrice price : sma) {
			de.tomsplayground.util.Day day = price.getDay();
			a1.addOrUpdate(new Day(day.day, day.month+1, day.year), price.getValue());
		}
	}

	private void createStopLoss(TimeSeries a1, StopLoss stopLoss) {
		for (Price price : stopLoss.getPrices(priceProvider)) {
			de.tomsplayground.util.Day day = price.getDay();
			a1.addOrUpdate(new Day(day.day, day.month+1, day.year), price.getValue());
		}
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
		displayType.setFocus();
	}

}
