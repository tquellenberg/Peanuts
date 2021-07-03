package de.tomsplayground.peanuts.client.editors.securitycategory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.Map;

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
import de.tomsplayground.peanuts.client.util.UniqueAsyncExecution;
import de.tomsplayground.peanuts.client.widgets.DateComposite;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.domain.reporting.investment.AnalyzerFactory;
import de.tomsplayground.peanuts.domain.reporting.transaction.Report;
import de.tomsplayground.peanuts.domain.statistics.SecurityCategoryMapping;
import de.tomsplayground.peanuts.util.Day;

public class PieEditorPart extends EditorPart {

	private Day date = Day.today();

	private JFreeChart chart;
	private Inventory inventory;
	private DefaultPieDataset<String> dataset;

	private final PropertyChangeListener inventoryChangeListener = new UniqueAsyncExecution() {
		@Override
		public void doit(PropertyChangeEvent evt, Display display) {
			updateDataset();
		}

		@Override
		public Display getDisplay() {
			return getSite().getShell().getDisplay();
		}
	};

	private ChartComposite chartFrame;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if ( !(input instanceof SecurityCategoryEditorInput)) {
			throw new PartInitException("Invalid Input: Must be SecurityCategoryEditorInput");
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

		Report report = new Report("temp");
		report.setAccounts(Activator.getDefault().getAccountManager().getAccounts());
		inventory = new Inventory(report, PriceProviderFactory.getInstance(), new AnalyzerFactory());
		inventory.addPropertyChangeListener(inventoryChangeListener);

		dataset = new DefaultPieDataset<String>();
		updateDataset();
		createChart();
		chartFrame = new ChartComposite(top, SWT.NONE, chart, true);
		chartFrame.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		dateChooser.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				date = dateChooser.getDay();
				inventory.setDate(date);
				updateDataset();
			}
		});
	}

	public void updateDataset() {
		SecurityCategoryMapping securityCategoryMapping = ((SecurityCategoryEditorInput) getEditorInput()).getSecurityCategoryMapping();
		dataset.clear();

		Map<String, BigDecimal> values = securityCategoryMapping.calculateCategoryValues(inventory);
		for (Map.Entry<String, BigDecimal> entry : values.entrySet()) {
			if (entry.getValue().intValue() != 0) {
				dataset.setValue(entry.getKey(), entry.getValue());
			}
		}
	}

	private void createChart() {
		chart = ChartFactory.createPieChart("", dataset, false, true, false);
		@SuppressWarnings("rawtypes")
		PiePlot plot = (PiePlot) chart.getPlot();
		plot.setForegroundAlpha(0.6f);
		plot.setCircular(true);
		plot.setBackgroundPaint(PeanutsDrawingSupplier.BACKGROUND_PAINT);
		plot.setLabelBackgroundPaint(java.awt.Color.WHITE);
		plot.setDefaultSectionOutlinePaint(PeanutsDrawingSupplier.GRIDLINE_PAINT);
		plot.setDefaultSectionPaint(PeanutsDrawingSupplier.GRIDLINE_PAINT);
		plot.setOutlinePaint(PeanutsDrawingSupplier.GRIDLINE_PAINT);
		plot.setDrawingSupplier(new PeanutsDrawingSupplier());
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