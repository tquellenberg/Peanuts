package de.tomsplayground.peanuts.client.editors.account;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.app.ib.IbFlexQuery;
import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.editors.ITransactionProviderInput;
import de.tomsplayground.peanuts.client.widgets.PersistentColumWidth;
import de.tomsplayground.peanuts.config.IConfigurable;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.ITransactionProvider;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.currenncy.ExchangeRates;
import de.tomsplayground.peanuts.domain.option.OptionsLog;
import de.tomsplayground.peanuts.domain.option.OptionsLog.Gain;
import de.tomsplayground.peanuts.domain.process.IPriceProviderFactory;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.domain.reporting.investment.AnalyzedInvestmentTransaction;
import de.tomsplayground.peanuts.domain.reporting.investment.AnalyzerFactory;
import de.tomsplayground.peanuts.domain.reporting.tax.RealizedGain;
import de.tomsplayground.peanuts.domain.statistics.SecurityCategoryMapping;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class TaxPart extends EditorPart {

	private final static Logger log = LoggerFactory.getLogger(TaxPart.class);

	public class RealizedEarningsTableLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider {

		private Color red;

		public RealizedEarningsTableLabelProvider(Color red) {
			this.red = red;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof AnalyzedInvestmentTransaction t) {
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
				if (columnIndex == 7 && lossesStockValues.containsKey(t)) {
					return PeanutsUtil.formatCurrency(lossesStockValues.get(t), account.getCurrency());
				}
				if (columnIndex == 8 && gainsStockValues.containsKey(t)) {
					return PeanutsUtil.formatCurrency(gainsStockValues.get(t), account.getCurrency());
				}
				if (columnIndex == 9 && sumStockValues.containsKey(t)) {
					return PeanutsUtil.formatCurrency(sumStockValues.get(t), account.getCurrency());
				}
			}
			if (element instanceof Gain gain) {
				if (columnIndex == 0) {
					return PeanutsUtil.formatDate(Day.from(gain.d().toLocalDate()));
				}
				if (columnIndex == 1) {
					return gain.option().getDescription();
				}
				if (columnIndex == 2) {
					return Integer.toString(gain.quantity());
				}
				if (columnIndex == 3) {
					return PeanutsUtil.formatCurrency(gain.pricePerOption(), Currency.getInstance("USD"));
				}
				if (columnIndex == 4) {
					return PeanutsUtil.formatCurrency(gain.commission(), Currency.getInstance("USD"));
				}
				if (columnIndex == 6) {
					return PeanutsUtil.formatCurrency(gain.gain(), account.getCurrency());
				}
				if (columnIndex == 7 && lossesOptionsValues.containsKey(gain)) {
					return PeanutsUtil.formatCurrency(lossesOptionsValues.get(gain), account.getCurrency());
				}
				if (columnIndex == 8 && gainOptionsValues.containsKey(gain)) {
					return PeanutsUtil.formatCurrency(gainOptionsValues.get(gain), account.getCurrency());
				}
				if (columnIndex == 9 && sumOptionsValues.containsKey(gain)) {
					return PeanutsUtil.formatCurrency(sumOptionsValues.get(gain), account.getCurrency());
				}
			}
			return null;
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			if (element instanceof AnalyzedInvestmentTransaction t) {
				if (columnIndex == 6 && t.getGain().signum() == -1) {
					return red;
				}
			}
			if (element instanceof Gain gain) {
				if (columnIndex == 6 && gain.gain().signum() == -1) {
					return red;
				}
			}
			return null;
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			return null;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
	}

	private ITransactionProvider account;

	private TableViewer tableViewer;

	private int selectedYear;

	private final Map<AnalyzedInvestmentTransaction, BigDecimal> lossesStockValues = new HashMap<>();
	private final Map<AnalyzedInvestmentTransaction, BigDecimal> gainsStockValues = new HashMap<>();
	private final Map<AnalyzedInvestmentTransaction, BigDecimal> sumStockValues = new HashMap<>();
	
	private final Map<Gain, BigDecimal> lossesOptionsValues = new HashMap<>();
	private final Map<Gain, BigDecimal> gainOptionsValues = new HashMap<>();
	private final Map<Gain, BigDecimal> sumOptionsValues = new HashMap<>();

	private RealizedGain realizedGain;

	private Inventory inventory;

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
		account = ((ITransactionProviderInput) getEditorInput()).getTransactionProvider();

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
		layout.numColumns = 3;
		banner.setLayout(layout);
		Font boldFont = Activator.getDefault().getBoldFont();

		Label l = new Label(banner, SWT.WRAP);
		l.setText("Year:");
		l.setFont(boldFont);

		final Combo yearCombo = new Combo(banner, SWT.DROP_DOWN | SWT.READ_ONLY);
		short startYear;
		if (! account.getTransactions().isEmpty()) {
			startYear = account.getTransactions().get(0).getDay().year;
		} else {
			startYear = Day.today().year;
		}
		for (int i = startYear; i <= Day.today().year; i++ ) {
			yearCombo.add(Integer.toString(i));
		}
		yearCombo.select(yearCombo.indexOf(Integer.toString(selectedYear)));
		yearCombo.addModifyListener(e -> {
			String yearString = ((Combo)e.widget).getText();
			if (StringUtils.isNotBlank(yearString) && StringUtils.isNumeric(yearString)) {
				selectedYear = Integer.parseInt(yearString);
				setStockData(true);
				saveState();
			}
		});
		
		Combo typeCombo = new Combo(banner, SWT.DROP_DOWN | SWT.READ_ONLY);
		typeCombo.add("Stocks");
		typeCombo.add("Other");
		typeCombo.add("Short Options");
		typeCombo.add("Long Options");
		typeCombo.select(0);
		typeCombo.addModifyListener(e -> {
			String selected = ((Combo)e.widget).getText();
			switch (selected) {
			case "Stocks": setStockData(true); break;
			case "Other": setStockData(false); break;
			case "Short Options": setOptionsData(false); break;
			case "Long Options": setOptionsData(true); break;
			default:
				throw new IllegalArgumentException("Unexpected value: " + selected);
			}
			tableViewer.refresh();
		});

		tableViewer = new TableViewer(top, SWT.FULL_SELECTION);
		Table table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setFont(Activator.getDefault().getNormalFont());

		TableColumn col = new TableColumn(table, SWT.RIGHT);
		col.setText("Date");
		col.setResizable(true);
		col.setWidth(100);

		col = new TableColumn(table, SWT.LEFT);
		col.setText("Name");
		col.setResizable(true);
		col.setWidth(100);

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Quantity");
		col.setResizable(true);
		col.setWidth(100);

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Price");
		col.setResizable(true);
		col.setWidth(100);

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Commision");
		col.setResizable(true);
		col.setWidth(100);

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Saldo");
		col.setResizable(true);
		col.setWidth(100);

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Gain/Loss");
		col.setResizable(true);
		col.setWidth(100);

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Loss sum");
		col.setResizable(true);
		col.setWidth(100);

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Gain sum");
		col.setResizable(true);
		col.setWidth(100);

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Total");
		col.setResizable(true);
		col.setWidth(100);

		new PersistentColumWidth(table, Activator.getDefault().getPreferenceStore(), 
				getClass().getCanonicalName()+"."+getEditorInput().getName());

		ColorRegistry colorProvider = Activator.getDefault().getColorProvider();
		Color red = colorProvider.get(Activator.RED);
		tableViewer.setLabelProvider(new RealizedEarningsTableLabelProvider(red));
		tableViewer.setContentProvider(new ArrayContentProvider());

		ExchangeRates exchangeRates = Activator.getDefault().getExchangeRates();
		IPriceProviderFactory priceProviderFactory = PriceProviderFactory.getInstance(account.getCurrency(), exchangeRates);
		inventory = new Inventory(account, priceProviderFactory, new AnalyzerFactory(), Activator.getDefault().getAccountManager());

		realizedGain = new RealizedGain(inventory);

		setStockData(true);
	}
	
	@Override
	public void dispose() {
		inventory.dispose();
		super.dispose();
	}

	private void setStockData(boolean isStock) {
		ImmutableList<AnalyzedInvestmentTransaction> realizedTransaction = realizedGain.getRealizedTransaction(selectedYear);
		realizedTransaction = ImmutableList.sortedCopyOf((a,b) -> a.getDay().compareTo(b.getDay()), realizedTransaction);

		AccountManager accountManager = Activator.getDefault().getAccountManager();
		SecurityCategoryMapping securityCategoryMapping = accountManager.getSecurityCategoryMapping("Typ");

		BigDecimal lossSum = BigDecimal.ZERO;
		BigDecimal gainSum = BigDecimal.ZERO;
		sumStockValues.clear();
		List<AnalyzedInvestmentTransaction> transToShow = new ArrayList<>();
		for (AnalyzedInvestmentTransaction t : realizedTransaction) {
			Security security = t.getSecurity();
			String category = securityCategoryMapping.getCategory(security);
			if (StringUtils.equalsAny(category, "Div Aktie", "Aktie") == isStock) {
				if (t.getGain().signum() == -1) {
					lossSum = lossSum.add(t.getGain());
					lossesStockValues.put(t, lossSum);
				} else {
					gainSum = gainSum.add(t.getGain());
					gainsStockValues.put(t, gainSum);
				}
				sumStockValues.put(t, gainSum.add(lossSum));
				transToShow.add(t);
			}
		}

		tableViewer.setInput(transToShow);
	}
	
	private void setOptionsData(boolean longTrade) {
		String filename = Activator.getDefault().getFilePath()+"/FlexQuery_"+selectedYear+".xml";
		log.info("Loading {}", filename);
		OptionsLog optionsFromXML = new IbFlexQuery().readOptionsFromXML(filename);
		List<Gain> gains = optionsFromXML.getGains().stream().filter(g -> g.longTrade() == longTrade).toList();
		
		BigDecimal lossSum = BigDecimal.ZERO;
		BigDecimal gainSum = BigDecimal.ZERO;
		sumOptionsValues.clear();
		for (Gain gain : gains) {
			if (gain.gain().signum() == -1) {
				lossSum = lossSum.add(gain.gain());
				lossesOptionsValues.put(gain, lossSum);
			} else {
				gainSum = gainSum.add(gain.gain());
				gainOptionsValues.put(gain, gainSum);
			}
			sumOptionsValues.put(gain, gainSum.add(lossSum));
		}
		
		tableViewer.setInput(gains);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	public void restoreState() {
		IConfigurable config = getEditorInput().getAdapter(IConfigurable.class);
		if (config != null) {
			String simpleClassName = getClass().getSimpleName();
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
