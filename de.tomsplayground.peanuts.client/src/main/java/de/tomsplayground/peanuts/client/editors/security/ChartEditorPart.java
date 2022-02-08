package de.tomsplayground.peanuts.client.editors.security;

import java.awt.BasicStroke;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
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
import org.jfree.chart.swt.ChartComposite;
import org.jfree.chart.ui.LengthAdjustmentType;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.Range;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.chart.JFreeChartFonts;
import de.tomsplayground.peanuts.client.chart.PeanutsDrawingSupplier;
import de.tomsplayground.peanuts.client.chart.TimeChart;
import de.tomsplayground.peanuts.client.editors.security.properties.ChartPropertyPage;
import de.tomsplayground.peanuts.client.util.UniqueAsyncExecution;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.base.InventoryEntry;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.domain.currenncy.Currencies;
import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;
import de.tomsplayground.peanuts.domain.currenncy.DummyCurrencyConverter;
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
	private static final String CHART_TYPE = "chartType";

	private static final Range peRatioChartRange = new Range(-35.0, 35.0);

	boolean dirty = false;
	private Combo displayType;
	private TimeSeries priceTimeSeries;
	private IPriceProvider priceProvider;
	private ChartComposite chartComposite;

	private final PropertyChangeListener priceProviderChangeListener = new UniqueAsyncExecution() {
		@Override
		public void doit(PropertyChangeEvent evt, Display display) {
			if (! chartComposite.isDisposed()) {
				fullChartUpdate();
			}
		}
		@Override
		public Display getDisplay() {
			return getSite().getShell().getDisplay();
		}
	};

	private final PropertyChangeListener securityPropertyChangeListener = new UniqueAsyncExecution() {
		@Override
		public void doit(PropertyChangeEvent evt, Display display) {
			if (! chartComposite.isDisposed()) {
				fullChartUpdate();
				return;
			}
		}
		@Override
		public Display getDisplay() {
			return getSite().getShell().getDisplay();
		}
	};

	private final PropertyChangeListener accountManagerChangeListener = new UniqueAsyncExecution() {
		@Override
		public void doit(PropertyChangeEvent evt, Display display) {
			if (chartComposite.isDisposed()) {
				return;
			}
			if (evt.getPropertyName().equals("stopLoss")) {
				updateStopLoss();
			}
		}
		@Override
		public Display getDisplay() {
			return getSite().getShell().getDisplay();
		}
	};

	private TimeSeries average20Days;
	private TimeSeries average100Days;
	// Upper chart
	private final TimeSeriesCollection dataset = new TimeSeriesCollection();
	// Lower chart
	private final TimeSeriesCollection dataset2 = new TimeSeriesCollection();
	private TimeChart timeChart;
	private TimeSeries stopLoss;
	private TimeSeries compareToPriceTimeSeries;
	private TimeSeries fixedPePrice;
	private XYPlot pricePlot;
	private ValueMarker avgPriceAnnotation;
	private TimeSeries peDeltaTimeSeries;

	private Currency alternativeCurrency;
	private Button convertToAlternativeCurrency;

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
		priceProvider = PriceProviderFactory.getInstance().getSplitAdjustedPriceProvider(security, stockSplits);
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

		addOrderAnnotations();

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
		timeChart.setChartType(chartType, de.tomsplayground.peanuts.util.Day.today());
		displayType.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				Combo c = (Combo)e.getSource();
				String type = c.getItem(c.getSelectionIndex());
				timeChart.setChartType(type, de.tomsplayground.peanuts.util.Day.today());
				dirty = true;
				firePropertyChange(IEditorPart.PROP_DIRTY);
				calculateCompareToValues();
			}
		});

		alternativeCurrency = security.getFundamentalDatas().getCurrency();
		if (security.getCurrency().equals(alternativeCurrency)) {
			if (security.getCurrency().equals(Currencies.getInstance().getDefaultCurrency())) {
				alternativeCurrency = Currency.getInstance("USD");
			} else {
				alternativeCurrency = Currencies.getInstance().getDefaultCurrency();
			}
		}
		Label text = new Label(buttons, SWT.NONE);
		text.setText("Convert from "+security.getCurrency().getCurrencyCode()+" to "+alternativeCurrency.getCurrencyCode());
		convertToAlternativeCurrency = new Button(buttons, SWT.CHECK);
		convertToAlternativeCurrency.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fullChartUpdate();
			}
		});

		calculateCompareToValues();

		security.addPropertyChangeListener(securityPropertyChangeListener);

		Activator.getDefault().getAccountManager().addPropertyChangeListener(accountManagerChangeListener);
	}
	
	private void fullChartUpdate() {
		createDataset();
		pricePlot.clearAnnotations();
		addOrderAnnotations();
		if (avgPriceAnnotation != null) {
			pricePlot.removeRangeMarker(avgPriceAnnotation);
		}
		addAvgPriceAnnotation();
		calculateCompareToValues();
		pricePlot.getRangeAxis().setLabel("Price "+getInventoryCurrencyConverter().getToCurrency().getSymbol());
		timeChart.setChartType(displayType.getItem(displayType.getSelectionIndex()), de.tomsplayground.peanuts.util.Day.today());
	}

	private void addAvgPriceAnnotation() {
		BigDecimal avgPrice = getAvgPrice();
		if (avgPrice != null && isShowBuyAndSell()) {
			avgPriceAnnotation = new ValueMarker(avgPrice.doubleValue());
			avgPriceAnnotation.setPaint(Color.black);
			avgPriceAnnotation.setLabelPaint(Color.black);
			avgPriceAnnotation.setLabel("Avg Price");
			avgPriceAnnotation.setLabelFont(JFreeChartFonts.getTickLabelFont());
			avgPriceAnnotation.setLabelOffsetType(LengthAdjustmentType.EXPAND);
			avgPriceAnnotation.setLabelAnchor(RectangleAnchor.TOP_LEFT);
			avgPriceAnnotation.setLabelTextAnchor(TextAnchor.BOTTOM_LEFT);
			avgPriceAnnotation.setLabelBackgroundColor((Color) PeanutsDrawingSupplier.BACKGROUND_PAINT);

			pricePlot.addRangeMarker(avgPriceAnnotation);
		} else {
			avgPriceAnnotation = null;
		}
	}

	private BigDecimal getAvgPrice() {
		Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();
		Inventory inventory = Activator.getDefault().getAccountManager().getFullInventory();
		if (inventory.getSecurities().contains(security)) {
			BigDecimal avgPrice = inventory.getEntry(security).getAvgPrice();
			return getInventoryCurrencyConverter().convert(avgPrice, de.tomsplayground.peanuts.util.Day.today());
		}
		return null;
	}

	/**
	 * Convert price data of the security.
	 * The original currency of the security chart data may differ from the currency of orders etc.
	 */
	private IPriceProvider getChartPriceProvider() {
		if (convertToAlternativeCurrency != null && convertToAlternativeCurrency.getSelection()) {
			Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();
			ExchangeRates exchangeRates = Activator.getDefault().getExchangeRates();
			CurrencyConverter currencyConverter = exchangeRates.createCurrencyConverter(security.getCurrency(), alternativeCurrency);
			if (currencyConverter != null) {
				return new CurrencyAdjustedPriceProvider(priceProvider, currencyConverter);
			}
		}
		return priceProvider;
	}

	/**
	 * Currency converter for inventory based values, e.g. orders or average price.
	 * TODO: inventory should use different currencies and not default currency only.
	 * Not used for the price data. See {@link #getChartPriceProvider()}
	 */
	private CurrencyConverter getInventoryCurrencyConverter() {
		Currency inventoryCurrency = Currencies.getInstance().getDefaultCurrency();
		ExchangeRates exchangeRates = Activator.getDefault().getExchangeRates();

		// Currency conversion active: inventory-currency to alternative currency
		if (convertToAlternativeCurrency != null && convertToAlternativeCurrency.getSelection()) {
			CurrencyConverter currencyConverter = exchangeRates.createCurrencyConverter(inventoryCurrency, alternativeCurrency);
			if (currencyConverter != null) {
				return currencyConverter;
			}
		}

		// Conversion from inventory-currency to security currency
		Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();
		if (! inventoryCurrency.equals(security.getCurrency())) {
			return exchangeRates.createCurrencyConverter(inventoryCurrency, security.getCurrency());
		}

		// No conversion
		return new DummyCurrencyConverter(inventoryCurrency);
	}

	protected void addSplitAnnotations(List<StockSplit> splits) {
		for (StockSplit stockSplit : splits) {
			de.tomsplayground.peanuts.util.Day day = stockSplit.getDay();
			long x = new Day(day.day, day.month+1, day.year).getFirstMillisecond();
			ValueMarker valueMarker = new ValueMarker(x);
			valueMarker.setLabelTextAnchor(TextAnchor.BOTTOM_RIGHT);
			valueMarker.setLabelAnchor(RectangleAnchor.BOTTOM_LEFT);
			valueMarker.setLabel("Split "+stockSplit.getFrom()+":"+stockSplit.getTo());
			pricePlot.addDomainMarker(valueMarker);
		}
	}

	private BigDecimal getSplitRatio(de.tomsplayground.peanuts.util.Day day) {
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
			de.tomsplayground.peanuts.util.Day day = investmentTransaction.getDay();
			long x = new Day(day.day, day.month+1, day.year).getFirstMillisecond();
			double y = pp.getPrice(day).getValue().doubleValue();

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
					adjustedPrice = getInventoryCurrencyConverter().convert(adjustedPrice, day);
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
					adjustedPrice = getInventoryCurrencyConverter().convert(adjustedPrice, day);
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
		axis.setDateFormatOverride(new SimpleDateFormat("MMM yyyy"));
		axis.setTickLabelFont(JFreeChartFonts.getTickLabelFont());
		CombinedDomainXYPlot combiPlot = new CombinedDomainXYPlot(axis);
		combiPlot.setDrawingSupplier(new PeanutsDrawingSupplier());
		JFreeChart chart = new JFreeChart(getEditorInput().getName(), combiPlot);
		chart.setBackgroundPaint(Color.WHITE);

		StandardXYItemRenderer renderer = new StandardXYItemRenderer();
		renderer.setDefaultToolTipGenerator(StandardXYToolTipGenerator.getTimeSeriesInstance());
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

		NumberAxis rangeAxis2 = new NumberAxis("Price "+getInventoryCurrencyConverter().getToCurrency().getSymbol());
		rangeAxis2.setAutoRange(false);
		rangeAxis2.setTickLabelFont(JFreeChartFonts.getTickLabelFont());
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
			xyAreaRenderer.setDefaultToolTipGenerator(StandardXYToolTipGenerator.getTimeSeriesInstance());
			xyAreaRenderer.setSeriesPaint(0, new PeanutsDrawingSupplier().getNextPaint());
			NumberAxis rangeAxis = new NumberAxis("PE delta %");
			rangeAxis.setAutoRange(false);
			rangeAxis.setRange(peRatioChartRange);
			rangeAxis.setTickLabelFont(JFreeChartFonts.getTickLabelFont());

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
		ExchangeRates exchangeRates = Activator.getDefault().getExchangeRates();
		BigDecimal avgPE = fundamentalDatas.getOverriddenAvgPE();
		if (avgPE == null) {
			AvgFundamentalData avgData = fundamentalDatas.getAvgFundamentalData(priceProvider, exchangeRates);
			avgPE = avgData.getAvgPE();
		}
		IPriceProvider adjustedPricePorvider = getChartPriceProvider();
		for (IPrice price : adjustedPricePorvider.getPrices()) {
			de.tomsplayground.peanuts.util.Day day = price.getDay();
			BigDecimal pe = fundamentalDatas.getAdjustedContinuousEarnings(day, adjustedPricePorvider.getCurrency(), exchangeRates);
			if (pe != null && pe.signum() > 0) {
				BigDecimal fairPrice = pe.multiply(avgPE, PeanutsUtil.MC);
				// Main Chart
				fixedPePrice.addOrUpdate(new Day(day.day, day.month+1, day.year), fairPrice);
				if (fairPrice.signum() == 1) {
					// Delta Chart
					BigDecimal deltaPercent = price.getValue().subtract(fairPrice).multiply(HUNDRED).divide(fairPrice, PeanutsUtil.MC);
					peDeltaTimeSeries.addOrUpdate(new Day(day.day, day.month+1, day.year), deltaPercent.doubleValue());
				}
			}
		}
	}

	private void createDataset() {
		dataset.removeAllSeries();
		dataset2.removeAllSeries();

		priceTimeSeries = new TimeSeries(getEditorInput().getName());
		for (IPrice price : getChartPriceProvider().getPrices()) {
			de.tomsplayground.peanuts.util.Day day = price.getDay();
			priceTimeSeries.add(new Day(day.day, day.month+1, day.year), price.getValue());
		}
		dataset.addSeries(priceTimeSeries);

		average20Days = new TimeSeries("MA20");
		average100Days = new TimeSeries("MA100");

		if (isShowAvg()) {
			createMovingAverage(average20Days, 20);
			dataset.addSeries(average20Days);
			createMovingAverage(average100Days, 100);
			dataset.addSeries(average100Days);
		}

		stopLoss = new TimeSeries("Stop Loss");
		updateStopLoss();
		dataset.addSeries(stopLoss);

		Security compareTo = getCompareTo();
		if (compareTo != null) {
			compareToPriceTimeSeries = new TimeSeries(compareTo.getName());
			dataset.addSeries(compareToPriceTimeSeries);
		}

		fixedPePrice = new TimeSeries("EPS * avg PE");
		peDeltaTimeSeries = new TimeSeries("PE delta %");
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
		if (compareToPriceTimeSeries != null) {
			compareToPriceTimeSeries.clear();
		}
		if (compareTo != null && timeChart.getFromDate() != null) {
			de.tomsplayground.peanuts.util.Day fromDate = timeChart.getFromDate();
			ImmutableList<StockSplit> stockSplits = Activator.getDefault().getAccountManager().getStockSplits(compareTo);
			IPriceProvider compareToPriceProvider = PriceProviderFactory.getInstance().getSplitAdjustedPriceProvider(compareTo, stockSplits);
			if (compareToPriceProvider.getMinDate().after(fromDate)) {
				fromDate = compareToPriceProvider.getMinDate();
			}
			IPrice p1 = priceProvider.getPrice(fromDate);
			IPrice p2 = compareToPriceProvider.getPrice(fromDate);
			BigDecimal adjust = p1.getValue().divide(p2.getValue(), PeanutsUtil.MC);
			for (IPrice price : compareToPriceProvider.getPrices()) {
				de.tomsplayground.peanuts.util.Day day = price.getDay();
				BigDecimal value = price.getValue().multiply(adjust);
				compareToPriceTimeSeries.add(new Day(day.day, day.month+1, day.year), value);
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
		List<IPrice> sma = simpleMovingAverage.calculate(getChartPriceProvider().getPrices());
		for (IPrice price : sma) {
			de.tomsplayground.peanuts.util.Day day = price.getDay();
			a1.addOrUpdate(new Day(day.day, day.month+1, day.year), price.getValue());
		}
	}

	private void createStopLoss(TimeSeries a1, StopLoss stopLoss) {
		for (Price price : stopLoss.getPrices(priceProvider)) {
			de.tomsplayground.peanuts.util.Day day = price.getDay();
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
