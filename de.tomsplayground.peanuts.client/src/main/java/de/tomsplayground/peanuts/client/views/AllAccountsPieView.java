package de.tomsplayground.peanuts.client.views;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.part.ViewPart;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.swt.ChartComposite;
import org.jfree.chart.util.SortOrder;
import org.jfree.data.general.DefaultPieDataset;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.chart.PeanutsDrawingSupplier;
import de.tomsplayground.peanuts.client.util.UniqueAsyncExecution;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.currenncy.Currencies;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.util.Day;

public class AllAccountsPieView extends ViewPart {

	private static final BigDecimal THRESHOLD = new BigDecimal("0.01");

	public static final String ID = "de.tomsplayground.peanuts.client.AllAccountsPieView";

	private DefaultPieDataset<String> dataset;
	private JFreeChart chart;
	private final Map<Account, Inventory> inventories = new HashMap<Account, Inventory>();
	private Shell shell;

	private final PropertyChangeListener accountChangeListener = new UniqueAsyncExecution() {
		@Override
		public Display getDisplay() {
			return shell.getDisplay();
		}

		@Override
		public void doit(PropertyChangeEvent evt, Display display) {
			updateControls();
		}
	};

	private ChartComposite chartFrame;

	@Override
	public void createPartControl(Composite parent) {
		shell = parent.getShell();

		dataset = new DefaultPieDataset<String>();
		createChart();
		chartFrame = new ChartComposite(parent, SWT.NONE, chart, true);
		chartFrame.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Activator.getDefault().getAccountManager().addPropertyChangeListener(accountChangeListener);
		updateControls();
	}

	@Override
	public void dispose() {
		Activator.getDefault().getAccountManager().removePropertyChangeListener(accountChangeListener);
		for (Inventory i : inventories.values()) {
			i.removePropertyChangeListener(accountChangeListener);
			i.dispose();
		}
	}

	protected void updateControls() {
		final Day today = Day.today();
		for (Account entry : Activator.getDefault().getAccountManager().getAccounts()) {
			Inventory i;
			if (inventories.containsKey(entry)) {
				i = inventories.get(entry);
				i.setDate(today);
			} else {
				i = new Inventory(entry, 
						PriceProviderFactory.getInstance(Currencies.getInstance().getDefaultCurrency(), Activator.getDefault().getExchangeRates()), 
						null,
						Activator.getDefault().getAccountManager());
				inventories.put(entry, i);
				i.addPropertyChangeListener(accountChangeListener);
			}
			BigDecimal sum = entry.getBalance(today).add(i.getMarketValue());
			if (sum.abs().compareTo(THRESHOLD) > 0) {
				dataset.setValue(entry.getName(), sum);
			}
		}
		dataset.sortByValues(SortOrder.DESCENDING);
	}

	private void createChart() {
		chart = ChartFactory.createPieChart("", dataset, false, true, false);
		@SuppressWarnings("rawtypes")
		PiePlot plot = (PiePlot) chart.getPlot();
		plot.setForegroundAlpha(0.6f);
		plot.setCircular(true);
		plot.setBackgroundPaint(PeanutsDrawingSupplier.BACKGROUND_PAINT);
		plot.setLabelBackgroundPaint(Color.WHITE);
		plot.setDefaultSectionOutlinePaint(PeanutsDrawingSupplier.GRIDLINE_PAINT);
		plot.setDefaultSectionPaint(PeanutsDrawingSupplier.GRIDLINE_PAINT);
		plot.setOutlinePaint(PeanutsDrawingSupplier.GRIDLINE_PAINT);
		plot.setDrawingSupplier(new PeanutsDrawingSupplier());
	}

	@Override
	public void setFocus() {
		chartFrame.setFocus();
	}

}
