package de.tomsplayground.peanuts.client.editors.account;

import static de.tomsplayground.peanuts.client.util.MinQuantity.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.swt.ChartComposite;
import org.jfree.data.general.DefaultPieDataset;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.chart.PeanutsDrawingSupplier;
import de.tomsplayground.peanuts.client.editors.ITransactionProviderInput;
import de.tomsplayground.peanuts.client.util.UniqueAsyncExecution;
import de.tomsplayground.peanuts.client.widgets.DateComposite;
import de.tomsplayground.peanuts.domain.base.ITransactionProvider;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.base.InventoryEntry;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.domain.reporting.investment.AnalyzerFactory;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class InventoryPieEditorPart extends EditorPart {

	private Label gainingLabel;
	private Label marketValueLabel;
	private Day date = Day.today();

	private Inventory inventory;
	private JFreeChart chart;
	private DefaultPieDataset<String> dataset;

	private ChartComposite chartFrame;

	private final PropertyChangeListener inventoryChangeListener = new UniqueAsyncExecution() {

		@Override
		public void doit(PropertyChangeEvent evt, Display display) {
			updateAll();
		}

		@Override
		public Display getDisplay() {
			return getSite().getShell().getDisplay();
		}
	};
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if ( !(input instanceof ITransactionProviderInput)) {
			throw new PartInitException("Invalid Input: Must be ITransactionProviderInput");
		}
		setSite(site);
		setInput(input);
		setPartName(input.getName());
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite top = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		top.setLayout(layout);
		// top banner
		Composite banner = new Composite(top, SWT.NONE);
		banner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		layout = new GridLayout();
		layout.marginHeight = 5;
		layout.marginWidth = 10;
		layout.numColumns = 2;
		banner.setLayout(layout);
		Font boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);

		Label l = new Label(banner, SWT.WRAP);
		l.setText("Date:");
		l.setFont(boldFont);
		final DateComposite dateChooser = new DateComposite(banner, SWT.NONE);

		l = new Label(banner, SWT.NONE);
		l.setText("Unrealized:");
		l.setFont(boldFont);
		gainingLabel = new Label(banner, SWT.NONE);
		gainingLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		l = new Label(banner, SWT.NONE);
		l.setFont(boldFont);
		l.setText("Market value:");
		marketValueLabel =  new Label(banner, SWT.NONE);
		marketValueLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		ITransactionProvider transactions = ((ITransactionProviderInput) getEditorInput()).getTransactionProvider();
		inventory = new Inventory(transactions, PriceProviderFactory.getInstance(), new AnalyzerFactory(), Activator.getDefault().getAccountManager());
		inventory.setDate(date);
		inventory.addPropertyChangeListener(inventoryChangeListener);

		dataset = new DefaultPieDataset<String>();
		createChart();
		chartFrame = new ChartComposite(top, SWT.NONE, chart, true);
		chartFrame.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		dateChooser.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				date = dateChooser.getDay();
				inventory.setDate(date);
				updateAll();
			}
		});

		updateAll();
	}

	private void updateDataset() {
		dataset.clear();
		List<InventoryEntry> entries = new ArrayList<InventoryEntry>(inventory.getEntries());
		Collections.sort(entries, new Comparator<InventoryEntry>() {
			@Override
			public int compare(InventoryEntry o1, InventoryEntry o2) {
				return o2.getMarketValue().compareTo(o1.getMarketValue());
			}
		});
		for (InventoryEntry entry : entries) {
			if (isNotZero(entry.getQuantity())) {
				dataset.setValue(entry.getSecurity().getName(), entry.getMarketValue());
			}
		}
	}

	private void createChart() {
		chart = ChartFactory.createPieChart("", dataset, false, true, false);
		@SuppressWarnings("rawtypes")
		PiePlot plot = (PiePlot) (chart.getPlot());
		plot.setForegroundAlpha(0.6f);
		plot.setCircular(true);
		plot.setBackgroundPaint(PeanutsDrawingSupplier.BACKGROUND_PAINT);
		plot.setLabelBackgroundPaint(java.awt.Color.WHITE);
		plot.setDefaultSectionOutlinePaint(PeanutsDrawingSupplier.GRIDLINE_PAINT);
		plot.setDefaultSectionPaint(PeanutsDrawingSupplier.GRIDLINE_PAINT);
		plot.setOutlinePaint(PeanutsDrawingSupplier.GRIDLINE_PAINT);
		plot.setDrawingSupplier(new PeanutsDrawingSupplier());
	}

	protected void updateAll() {
		ITransactionProvider transactions = ((ITransactionProviderInput) getEditorInput()).getTransactionProvider();
		gainingLabel.setText(PeanutsUtil.formatCurrency(inventory.getUnrealizedGainings(), transactions.getCurrency()));
		marketValueLabel.setText(PeanutsUtil.formatCurrency(inventory.getMarketValue(), transactions.getCurrency()));
		marketValueLabel.getParent().layout();
		updateDataset();
	}

	@Override
	public void dispose() {
		inventory.removePropertyChangeListener(inventoryChangeListener);
		inventory.dispose();
		super.dispose();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// nothing to do
	}

	@Override
	public void doSaveAs() {
		// nothing to do
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void setFocus() {
		chartFrame.setFocus();
	}

}
