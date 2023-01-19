package de.tomsplayground.peanuts.client.comparison;

import java.awt.BasicStroke;
import java.awt.Color;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.swt.ChartComposite;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.chart.JFreeChartFonts;
import de.tomsplayground.peanuts.client.widgets.SecurityProposalText;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class ComparisonEditor extends EditorPart {

	public static final String ID = "de.tomsplayground.peanuts.client.comparisonEditor";

	private static final float BASIC_WIDTH = 1.0f;
	private static final float BOLD_WIDTH = 3.0f;

	private static final BasicStroke BASIC_STROKE = new BasicStroke(BASIC_WIDTH);
	private static final BasicStroke BOLD_STROKE = new BasicStroke(BOLD_WIDTH);

	public final static List<Day> START_DAYS = List.of(Day.of(2020, Month.FEBRUARY, 1), 
			Day.of(2020, Month.MARCH, 23),
			Day.of(2020, Month.SEPTEMBER, 2), 
			Day.of(2020, Month.OCTOBER, 30),  
			Day.of(2022, Month.OCTOBER, 1),
			Day.firstDayOfYear(Day.today().year-1), // Start of last year
			Day.firstDayOfYear(Day.today().year), // Start of current year
			Day.today().addMonth(-6),  // 6 month 
			Day.today().addMonth(-3),  // 3 month 
			Day.today().addMonth(-1),  // 1 month
			Day.today().addDays(-14));

	private boolean dirty;

	private ChartComposite chartFrame;

	private TableViewer tableViewer;

	private final int colWidth[] = new int[2];

	private SecurityProposalText securityProposalText;

	private Security selectedSecurity = null;

	private StandardXYItemRenderer renderer;

	private TimeSeriesCollection dataset;

	private List<Security> securities;

	private Combo startDateCombo;

	private static class SecurityTableLabelProvider extends LabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			Security security = (Security) element;
			 return switch (columnIndex) {
				case 0 -> "";
				case 1 -> security.getName();
				default -> "";
			};
		}
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if ( !(input instanceof ComparisonInput)) {
			throw new PartInitException("Invalid Input: Must be ComparisonInput");
		}
		setSite(site);
		setInput(input);
		setPartName(input.getName());
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite body = new Composite(parent, SWT.BORDER);
		body.setLayout(new GridLayout(2, false));

		JFreeChart chart = createChart();
		chartFrame = new ChartComposite(body, SWT.NONE, chart, true);
		chartFrame.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite cntrlComposite = new Composite(body, SWT.BORDER);
		cntrlComposite.setLayout(new GridLayout());
		GridData gridData = new GridData(SWT.LEFT, SWT.TOP, false, false);
		gridData.widthHint = 200;
		cntrlComposite.setLayoutData(gridData);

		tableViewer = new TableViewer(cntrlComposite, SWT.FULL_SELECTION);
		Table table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setFont(Activator.getDefault().getNormalFont());

		TableColumn col = new TableColumn(table, SWT.LEFT);
		col.setText("State");
		col.setWidth((colWidth[0] > 0) ? colWidth[0] : 30);
		col.setResizable(true);

		col = new TableColumn(table, SWT.LEFT);
		col.setText("Name");
		col.setWidth((colWidth[1] > 0) ? colWidth[1] : 170);
		col.setResizable(true);

		tableViewer.setLabelProvider(new SecurityTableLabelProvider());
		tableViewer.setContentProvider(new ArrayContentProvider());

		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object firstElement = event.getStructuredSelection().getFirstElement();
				if (firstElement == null) {
					deselectChart();
				} else {
					selectChart((Security)firstElement);
				}
			}
		});

		securities = getComparisonInput().getComparison().getSecurities();
		if (securities == null) {
			securities = new ArrayList<>();
		}
		tableViewer.setInput(securities);
		MenuManager menuManager = new MenuManager();
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}

		});
		tableViewer.getTable().setMenu(menuManager.createContextMenu(table));

		startDateCombo = new Combo(cntrlComposite, SWT.READ_ONLY);
		for (Day d : START_DAYS) {
			startDateCombo.add(d.toString());
		}
		startDateCombo.select(0);
		startDateCombo.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				redrawChart();
			}
		});

		securityProposalText = new SecurityProposalText(cntrlComposite);
		securityProposalText.getText().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Button button = new Button(cntrlComposite, SWT.PUSH);
		button.setText("Add");
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String securiytName = securityProposalText.getText().getText();
				Optional<Security> findAny = Activator.getDefault().getAccountManager().getSecurities().stream()
					.filter(s -> s.getName().equals(securiytName))
					.findAny();
				if (findAny.isPresent()) {
					Security securityToAdd = findAny.get();
					securities.add(securityToAdd);
					tableViewer.refresh();
					redrawChart();
					markDirty();
				}
			}
		});
		
		createTotalDataset(dataset);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(new Action("Remove") {
			@Override
			public void run() {
				ISelection selection = tableViewer.getSelection();
				if (! selection.isEmpty() && selection instanceof StructuredSelection ssel) {
					Security security = (Security) ssel.getFirstElement();
					securities.remove(security);
					selectedSecurity = null;
					tableViewer.refresh();
					redrawChart();
					markDirty();
				}
			}
		});
	}

	protected void selectChart(Security newSelectedSecurity) {
		deselectChart();
		int index = securities.indexOf(newSelectedSecurity);
		if (index >= 0) {
			renderer.setSeriesStroke(index, BOLD_STROKE);
			selectedSecurity = newSelectedSecurity;
		}
	}

	protected void deselectChart() {
		if (selectedSecurity != null) {
			int index = securities.indexOf(selectedSecurity);
			if (index >= 0) {
				renderer.setSeriesStroke(index, BASIC_STROKE);
			}
		}
		selectedSecurity = null;
	}

	private JFreeChart createChart() {
		dataset = new TimeSeriesCollection();
//		createTotalDataset(dataset);

		JFreeChart chart = ChartFactory.createTimeSeriesChart(
			getEditorInput().getName(), // title
			"Date", // x-axis label
			"Price", // y-axis label
			dataset, // data
			true, // create legend?
			true, // generate tooltips?
			false // generate URLs?
			);
		chart.setBackgroundPaint(Color.WHITE);

		renderer = new StandardXYItemRenderer();
		for (int i = 0; i < dataset.getSeriesCount(); i++) {
			renderer.setSeriesStroke(i, BASIC_STROKE);
		}
		renderer.setDefaultToolTipGenerator(StandardXYToolTipGenerator.getTimeSeriesInstance());

		XYPlot xyPlot = chart.getXYPlot();
		xyPlot.setBackgroundPaint(new Color(230, 230, 230));
		xyPlot.setDomainCrosshairVisible(true);
		xyPlot.setRangeCrosshairVisible(true);
		xyPlot.setRenderer(renderer);
		xyPlot.getRangeAxis().setTickLabelFont(JFreeChartFonts.getTickLabelFont());

		DateAxis axis = (DateAxis) xyPlot.getDomainAxis();
		axis.setDateFormatOverride(new SimpleDateFormat("MMM yyyy"));
		axis.setTickLabelFont(JFreeChartFonts.getTickLabelFont());

		return chart;
	}

	protected void redrawChart() {
		dataset.removeAllSeries();
		createTotalDataset(dataset);
	}

	private ComparisonInput getComparisonInput() {
		return (ComparisonInput) getEditorInput();
	}

	private Day getSelectedStartDate() {
		return START_DAYS.get(startDateCombo.getSelectionIndex());
	}
	
	private TimeSeriesCollection createTotalDataset(TimeSeriesCollection dataset) {
		for (Security security : securities) {
			Day start = getSelectedStartDate();
			Day end = Day.today();
			TimeSeries series = new TimeSeries(StringUtils.abbreviate(security.getName(), 18));
			IPriceProvider priceProvider = PriceProviderFactory.getInstance().getPriceProvider(security);
			BigDecimal startValue = priceProvider.getPrice(start).getValue();

			IPriceProvider basePriceProvider = null;
			BigDecimal startBaseValue = null;
			Security baseSecurity = getComparisonInput().getComparison().getBaseSecurity();
			if (baseSecurity != null) {
				basePriceProvider = PriceProviderFactory.getInstance().getPriceProvider(baseSecurity);
				startBaseValue = basePriceProvider.getPrice(start).getValue();
			}

			for (IPrice price : priceProvider.getPrices(start, end)) {
				Day priceDay = price.getDay();
				series.addOrUpdate(new org.jfree.data.time.Day(priceDay.day, priceDay.getMonth().getValue(), priceDay.year),
					calcPercentage(startValue, price, startBaseValue, basePriceProvider));
			}
			dataset.addSeries(series);
		}
		return dataset;
	}

	private BigDecimal calcPercentage(BigDecimal startValue, IPrice price, BigDecimal startBaseValue, IPriceProvider basePriceProvider) {
		BigDecimal percentage = price.getValue().divide(startValue, PeanutsUtil.MC).multiply(new BigDecimal(100));
		if (startBaseValue != null) {
			BigDecimal basePercentage = basePriceProvider.getPrice(price.getDay()).getValue().divide(startBaseValue, PeanutsUtil.MC).multiply(new BigDecimal(100));
			percentage = percentage.subtract(basePercentage);
		}
		return percentage;
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	public void markDirty() {
		dirty = true;
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void setFocus() {
	}

}
