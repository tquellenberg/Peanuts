package de.tomsplayground.peanuts.client.editors.account;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.editors.ITransactionProviderInput;
import de.tomsplayground.peanuts.config.IConfigurable;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.ITransactionProvider;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.currenncy.ExchangeRates;
import de.tomsplayground.peanuts.domain.process.CurrencyAdjustedPriceProviderFactory;
import de.tomsplayground.peanuts.domain.process.IPriceProviderFactory;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.domain.reporting.investment.AnalyzedInvestmentTransaction;
import de.tomsplayground.peanuts.domain.reporting.investment.AnalyzerFactory;
import de.tomsplayground.peanuts.domain.reporting.tax.RealizedGain;
import de.tomsplayground.peanuts.domain.statistics.SecurityCategoryMapping;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class TaxPart extends EditorPart {

	public class RealizedEarningsTableLabelProvider implements ITableLabelProvider {

		@Override
		public void addListener(ILabelProviderListener listener) {
		}

		@Override
		public void dispose() {
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof AnalyzedInvestmentTransaction) {
				AnalyzedInvestmentTransaction t = (AnalyzedInvestmentTransaction)element;
				if (columnIndex == 0) {
					return PeanutsUtil.formatDate(t.getDay());
				}
				if (columnIndex == 1) {
					return t.getSecurity().getName();
				}
				if (columnIndex == 2) {
					return PeanutsUtil.formatQuantity(t.getQuantity());
				}
				if (columnIndex == 3) {
					return PeanutsUtil.formatCurrency(t.getPrice(), account.getCurrency());
				}
				if (columnIndex == 4) {
					return PeanutsUtil.formatCurrency(t.getCommission(), account.getCurrency());
				}
				if (columnIndex == 5) {
					return PeanutsUtil.formatCurrency(t.getAmount(), account.getCurrency());
				}
				if (columnIndex == 6) {
					return PeanutsUtil.formatCurrency(t.getGain(), account.getCurrency());
				}
				if (columnIndex == 7 && sumStockValues != null) {
					return PeanutsUtil.formatCurrency(sumStockValues.get(t), account.getCurrency());
				}
				if (columnIndex == 8 && sumOtherValues != null) {
					return PeanutsUtil.formatCurrency(sumOtherValues.get(t), account.getCurrency());
				}
			}
			return null;
		}

	}

	private ITransactionProvider account;

	private final int colWidth[] = new int[11];
	private TableViewer tableViewer;

	private int selectedYear;

	private Map<AnalyzedInvestmentTransaction, BigDecimal> sumStockValues;

	private Map<AnalyzedInvestmentTransaction, BigDecimal> sumOtherValues;

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
		restoreState();

		Composite top = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		top.setLayout(layout);
		// top banner
		final Composite banner = new Composite(top, SWT.NONE);
		banner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		layout = new GridLayout();
		layout.marginHeight = 5;
		layout.marginWidth = 10;
		layout.numColumns = 2;
		banner.setLayout(layout);
		Font boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);

		Label l = new Label(banner, SWT.WRAP);
		l.setText("Year:");
		l.setFont(boldFont);

		final Combo yearCombo = new Combo(banner, SWT.DROP_DOWN | SWT.READ_ONLY);
		for (int i = 1990; i <= 2021; i++ ) {
			yearCombo.add(Integer.toString(i));
		}
		yearCombo.select(yearCombo.indexOf(Integer.toString(selectedYear)));
		yearCombo.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				final String yearString = ((Combo)e.widget).getText();
				if (StringUtils.isNotBlank(yearString) && StringUtils.isNumeric(yearString)) {
					selectedYear = Integer.parseInt(yearString);
					setData();
				}
			}
		});

		ControlListener saveSizeOnResize = new ControlListener() {
			@Override
			public void controlResized(ControlEvent e) {
				saveState();
			}
			@Override
			public void controlMoved(ControlEvent e) {
			}
		};

		tableViewer = new TableViewer(top, SWT.FULL_SELECTION);
		Table table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setFont(Activator.getDefault().getNormalFont());

		int colNumber = 0;

		TableColumn col = new TableColumn(table, SWT.RIGHT);
		col.setText("Date");
		col.setResizable(true);
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.addControlListener(saveSizeOnResize);
		colNumber++;

		col = new TableColumn(table, SWT.LEFT);
		col.setText("Name");
		col.setResizable(true);
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.addControlListener(saveSizeOnResize);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Quantity");
		col.setResizable(true);
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.addControlListener(saveSizeOnResize);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Price");
		col.setResizable(true);
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.addControlListener(saveSizeOnResize);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Commision");
		col.setResizable(true);
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.addControlListener(saveSizeOnResize);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Saldo");
		col.setResizable(true);
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.addControlListener(saveSizeOnResize);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Gain/Lost");
		col.setResizable(true);
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.addControlListener(saveSizeOnResize);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Sum stocks");
		col.setResizable(true);
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.addControlListener(saveSizeOnResize);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Sum others");
		col.setResizable(true);
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.addControlListener(saveSizeOnResize);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Gain/Lost (%)");
		col.setResizable(true);
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.addControlListener(saveSizeOnResize);
		colNumber++;

		tableViewer.setLabelProvider(new RealizedEarningsTableLabelProvider());
		tableViewer.setContentProvider(new ArrayContentProvider());

		setData();
	}

	private void setData() {
		account = ((ITransactionProviderInput) getEditorInput()).getTransactionProvider();
		IPriceProviderFactory priceProviderFactory = PriceProviderFactory.getInstance();
		ExchangeRates exchangeRates = Activator.getDefault().getExchangeRates();
		priceProviderFactory = new CurrencyAdjustedPriceProviderFactory(account.getCurrency(), priceProviderFactory, exchangeRates);
		Inventory inventory = new Inventory(account, priceProviderFactory, new AnalyzerFactory());

		RealizedGain realizedGain = new RealizedGain(inventory);
		ImmutableList<AnalyzedInvestmentTransaction> realizedTransaction = realizedGain.getRealizedTransaction(selectedYear);
		realizedTransaction = ImmutableList.sortedCopyOf((a,b) -> a.getDay().compareTo(b.getDay()), realizedTransaction);

		AccountManager accountManager = Activator.getDefault().getAccountManager();
		SecurityCategoryMapping securityCategoryMapping = accountManager.getSecurityCategoryMapping("Typ");

		BigDecimal sumStocks = BigDecimal.ZERO;
		BigDecimal sumOthers = BigDecimal.ZERO;
		sumStockValues = new HashMap<>();
		sumOtherValues = new HashMap<>();
		for (AnalyzedInvestmentTransaction t : realizedTransaction) {
			Security security = t.getSecurity();
			String category = securityCategoryMapping.getCategory(security);
			if (StringUtils.equalsAny(category, "Div Aktie", "Aktie")) {
				sumStocks = sumStocks.add(t.getGain());
				sumStockValues.put(t, sumStocks);
			} else {
				sumOthers = sumOthers.add(t.getGain());
				sumOtherValues.put(t, sumOthers);
			}
		}

		tableViewer.setInput(realizedTransaction);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	public void restoreState() {
		IConfigurable config = getEditorInput().getAdapter(IConfigurable.class);
		if (config != null) {
			String simpleClassName = getClass().getSimpleName();
			for (int i = 0; i < colWidth.length; i++ ) {
				String width = config.getConfigurationValue(simpleClassName+".col" + i);
				if (width != null) {
					colWidth[i] = Integer.valueOf(width).intValue();
				}
			}
			String selectedYearStr = config.getConfigurationValue(simpleClassName+".selectedYear");
			if (StringUtils.isNotBlank(selectedYearStr)) {
				selectedYear = Integer.valueOf(selectedYearStr);
			} else {
				selectedYear = Day.today().year;
			}
		}
	}

	public void saveState() {
		IConfigurable config = getEditorInput().getAdapter(IConfigurable.class);
		if (config != null) {
			String simpleClassName = getClass().getSimpleName();
			TableColumn[] columns = tableViewer.getTable().getColumns();
			for (int i = 0; i < columns.length; i++ ) {
				TableColumn tableColumn = columns[i];
				config.putConfigurationValue(simpleClassName+".col" + i, String.valueOf(tableColumn.getWidth()));
			}
			config.putConfigurationValue(simpleClassName+".selectedYear", Integer.toString(selectedYear));
		}
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void setFocus() {
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}


}
