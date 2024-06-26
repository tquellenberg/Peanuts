package de.tomsplayground.peanuts.client.editors.account;

import static de.tomsplayground.peanuts.client.util.MinQuantity.isNotZero;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.editors.ITransactionProviderInput;
import de.tomsplayground.peanuts.client.editors.security.SecurityEditor;
import de.tomsplayground.peanuts.client.editors.security.SecurityEditorInput;
import de.tomsplayground.peanuts.client.util.UniqueAsyncExecution;
import de.tomsplayground.peanuts.client.widgets.DateComposite;
import de.tomsplayground.peanuts.client.widgets.PersistentColumWidth;
import de.tomsplayground.peanuts.config.IConfigurable;
import de.tomsplayground.peanuts.domain.base.ITransactionProvider;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.base.InventoryEntry;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;
import de.tomsplayground.peanuts.domain.currenncy.ExchangeRates;
import de.tomsplayground.peanuts.domain.dividend.SecurityDividendStats;
import de.tomsplayground.peanuts.domain.fundamental.CurrencyAjustedFundamentalData;
import de.tomsplayground.peanuts.domain.fundamental.FundamentalData;
import de.tomsplayground.peanuts.domain.process.IPriceProviderFactory;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction.Type;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.domain.reporting.investment.AnalyzedInvestmentTransaction;
import de.tomsplayground.peanuts.domain.reporting.investment.AnalyzerFactory;
import de.tomsplayground.peanuts.domain.reporting.tax.RealizedGain;
import de.tomsplayground.peanuts.domain.statistics.SecurityCategoryMapping;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class InventoryEditorPart extends EditorPart {

	private static final String SHOW_ALL_SECURITIES = "inventoryShowAllSecurities";

	private TreeViewer treeViewer;
	private Label gainingLabel;
	private Label realizedLabel;
	private Label marketValueLabel;
	private Label changeLabel;
	private Day date = Day.today();

	private boolean showAllSecurities;
	private Inventory inventory;

	abstract private class InventoryComparator extends ViewerComparator {

		abstract int compareInventoryEntry(Viewer viewer, InventoryEntry e1, InventoryEntry e2);

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			int sort = ((TreeViewer)viewer).getTree().getSortDirection();
			if (e1 instanceof InventoryEntry  invE1 && e2 instanceof InventoryEntry invE2) {
				int compare = compareInventoryEntry(viewer, invE1, invE2);
				return (sort == SWT.DOWN) ? compare : -compare;
			}
			if (e1 instanceof InvestmentTransaction i1 && e2 instanceof InvestmentTransaction i2) {
				ImmutableList<InvestmentTransaction> transactions = inventory.getEntry(i1.getSecurity()).getTransactions();
				return Integer.compare(transactions.indexOf(i2), transactions.indexOf(i1));
			}
			return 0;
		}
	}

	private final InventoryComparator investmentSumComparator = new InventoryComparator() {
		@Override
		public int compareInventoryEntry(Viewer viewer, InventoryEntry i1, InventoryEntry i2) {
			return i1.getInvestedAmount().compareTo(i2.getInvestedAmount());
		}
	};

	private final InventoryComparator marketValueComparator = new InventoryComparator() {
		@Override
		public int compareInventoryEntry(Viewer viewer, InventoryEntry i1, InventoryEntry i2) {
			return i1.getMarketValue().compareTo(i2.getMarketValue());
		}
	};

	private final InventoryComparator securityNameComparator = new InventoryComparator() {
		@Override
		public int compareInventoryEntry(Viewer viewer, InventoryEntry i1, InventoryEntry i2) {
			return i2.getSecurity().getName().compareToIgnoreCase(i1.getSecurity().getName());
		}
	};

	private final InventoryComparator gainingComparator = new InventoryComparator() {
		private BigDecimal gaining(InventoryEntry entry) {
			return entry.getMarketValue().subtract(entry.getInvestedAmount());
		}
		@Override
		public int compareInventoryEntry(Viewer viewer, InventoryEntry i1, InventoryEntry i2) {
			return gaining(i1).compareTo(gaining(i2));
		}
	};

	private final InventoryComparator gainingPercentComparator = new InventoryComparator() {
		private BigDecimal gainingPercent(InventoryEntry entry) {
			if (entry.getInvestedAmount().compareTo(BigDecimal.ZERO) != 0) {
				BigDecimal gain = entry.getMarketValue().subtract(entry.getInvestedAmount());
				return gain.divide(entry.getInvestedAmount(), PeanutsUtil.MC);
			}
			return BigDecimal.ZERO;
		}

		@Override
		public int compareInventoryEntry(Viewer viewer, InventoryEntry i1, InventoryEntry i2) {
			return gainingPercent(i1).compareTo(gainingPercent(i2));
		}
	};

	private final InventoryComparator ratePerYearComparator = new InventoryComparator() {
		@Override
		public int compareInventoryEntry(Viewer viewer, InventoryEntry i1, InventoryEntry i2) {
			return i1.getXIRR().compareTo(i2.getXIRR());
		}
	};

	private final InventoryComparator dailyChangeComparator = new InventoryComparator() {
		@Override
		public int compareInventoryEntry(Viewer viewer, InventoryEntry i1, InventoryEntry i2) {
			return i1.getDayChange().compareTo(i2.getDayChange());
		}
	};


	private final PropertyChangeListener inventoryChangeListener = new UniqueAsyncExecution() {
		@Override
		public void doit(PropertyChangeEvent evt, Display display) {
			if (!treeViewer.getTree().isDisposed()) {
				updateAll();
			}
		}

		@Override
		public Display getDisplay() {
			return getSite().getShell().getDisplay();
		}
	};

//	private DividendStats dividendStats;

	private IPriceProviderFactory priceProviderFactory;

	private static class InventoryContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getElements(Object inputElement) {
			Inventory inventory = (Inventory) inputElement;
			List<InventoryEntry> entries = new ArrayList<InventoryEntry>(inventory.getEntries());
			return entries.toArray();
		}

		@Override
		public boolean hasChildren(Object element) {
			return element instanceof InventoryEntry;
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof InventoryEntry entry) {
				ImmutableList<InvestmentTransaction> transations = entry.getTransactions();
				return transations.toArray();
			}
			return null;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public void dispose() {
			// nothing to do
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// nothing to do
		}
	}

	private class InventoryLabelProvider implements ITableLabelProvider, ITableColorProvider {

		@SuppressWarnings("unused")
		private static final int INVENTORY_POS_NAME = 0;
		private static final int TRANSACTION_POS_DATE = 0;
		private static final int INVENTORY_FRACTION = 1;
		private static final int TRANSACTION_POS_QUANTITY = 1;
		private static final int INVENTORY_POS_QUANTITY = 2;
		private static final int TRANSACTION_POS_QUANTITY_SUM = 2;
		private static final int INVENTORY_POS_PRICE = 3;
		private static final int TRANSACTION_POS_PRICE = 3;
		private static final int INVENTORY_POS_MARKETVALUE = 4;
		private static final int TRANSACTION_POS_AMOUNT = 4;
		private static final int INVENTORY_POS_AVG_PRICE = 5;
		private static final int TRANSACTION_POS_AVG_PRICE = 5;
		private static final int INVENTORY_POS_INVESTED_SUM = 6;
		private static final int TRANSACTION_POS_INVESTED_SUM = 6;
		private static final int TRANSACTION_POS_GAIN = 7;
		private static final int INVENTORY_POS_GAIN = 7;
		private static final int INVENTORY_POS_GAIN_PERCENT = 8;
		private static final int INVENTORY_POS_RATE = 9;
		private static final int INVENTORY_POS_DAY_CHANGE = 10;
		private static final int INVENTORY_POS_YOC = 11;
		private static final int INVENTORY_POS_PAYED_DIVIDEND = 12;
		private static final int INVENTORY_POS_DIVIDEND_YIELD = 13;

		private final Currency currency;
		private final Color red;

		public InventoryLabelProvider(Color red, Currency currency) {
			this.red = red;
			this.currency = currency;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof InventoryEntry entry) {
				if (columnIndex == INVENTORY_FRACTION) {
					if (inventory.getMarketValue().compareTo(BigDecimal.ZERO) != 0) {
						BigDecimal fraction = entry.getMarketValue().divide(inventory.getMarketValue(), PeanutsUtil.MC);
						return PeanutsUtil.formatPercent(fraction);
					}
				}
				if (columnIndex == INVENTORY_POS_PRICE) {
					return PeanutsUtil.formatCurrency(entry.getPrice().getValue(), currency);
				}
				if (columnIndex == INVENTORY_POS_MARKETVALUE) {
					return PeanutsUtil.formatCurrency(entry.getMarketValue(), currency);
				}
				if (columnIndex == INVENTORY_POS_GAIN) {
					return PeanutsUtil.formatCurrency(entry.getMarketValue().subtract(entry.getInvestedAmount()), currency);
				}
				if (columnIndex == INVENTORY_POS_GAIN_PERCENT) {
					if (entry.getInvestedAmount().compareTo(BigDecimal.ZERO) != 0) {
						BigDecimal gain = entry.getMarketValue().subtract(entry.getInvestedAmount());
						BigDecimal gainPercent = gain.divide(entry.getInvestedAmount(), PeanutsUtil.MC);
						return PeanutsUtil.formatPercent(gainPercent);
					}
				}
				if (columnIndex == INVENTORY_POS_DAY_CHANGE) {
					return PeanutsUtil.formatCurrency(entry.getDayChange(), currency);
				}
				if (columnIndex == INVENTORY_POS_QUANTITY) {
					return PeanutsUtil.formatQuantity(entry.getQuantity());
				}
				if (columnIndex == INVENTORY_POS_AVG_PRICE) {
					return PeanutsUtil.formatCurrency(entry.getAvgPrice(), currency);
				}
				if (columnIndex == INVENTORY_POS_INVESTED_SUM) {
					return PeanutsUtil.formatCurrency(entry.getInvestedAmount(), currency);
				}
				if (columnIndex == INVENTORY_POS_RATE) {
					return PeanutsUtil.formatPercent(entry.getXIRR());
				}
				if (columnIndex == INVENTORY_POS_YOC) {
					BigDecimal dividendYoc = new SecurityDividendStats(entry.getSecurity()).getYoC(entry.getAvgPrice(), Activator.getDefault().getExchangeRates());
					if (dividendYoc.compareTo(BigDecimal.ZERO) != 0) {
						return PeanutsUtil.formatPercent(dividendYoc);
					}
				}
				if (columnIndex == INVENTORY_POS_PAYED_DIVIDEND) {
					BigDecimal dividendSum = new SecurityDividendStats(entry.getSecurity())
							.getFutureDividendSum(entry.getQuantity(), Activator.getDefault().getExchangeRates());
					if (dividendSum.compareTo(BigDecimal.ZERO) != 0) {
						return PeanutsUtil.formatCurrency(dividendSum, currency);
					}
				}
				if (columnIndex == INVENTORY_POS_DIVIDEND_YIELD) {
					BigDecimal dividendYield = BigDecimal.ZERO;
					Security security = entry.getSecurity();
					FundamentalData fundamentalData = security.getFundamentalDatas().getCurrentFundamentalData();
					if (fundamentalData != null) {
						ExchangeRates exchangeRates = Activator.getDefault().getExchangeRates();
						CurrencyConverter currencyConverter = exchangeRates.createCurrencyConverter(fundamentalData.getCurrency(), getTransactions().getCurrency());
						CurrencyAjustedFundamentalData currencyAjustedFundamentalData = new CurrencyAjustedFundamentalData(fundamentalData, currencyConverter);
						
						dividendYield = currencyAjustedFundamentalData.calculateDivYield(priceProviderFactory.getPriceProvider(security));
					}
					if (dividendYield.compareTo(BigDecimal.ZERO) != 0) {
						return PeanutsUtil.formatPercent(dividendYield);
					}
				}
				
			} else if (element instanceof AnalyzedInvestmentTransaction t) {
				if (columnIndex == TRANSACTION_POS_QUANTITY) {
					if (t.getType() == InvestmentTransaction.Type.BUY) {
						return PeanutsUtil.formatQuantity(t.getQuantity());
					}
					return PeanutsUtil.formatQuantity(t.getQuantity().negate());
				}
				if (columnIndex == TRANSACTION_POS_PRICE) {
					return PeanutsUtil.formatCurrency(t.getPrice(), currency);
				}
				if (columnIndex == TRANSACTION_POS_AMOUNT) {
					return PeanutsUtil.formatCurrency(t.getAmount(), currency);
				}
				if (columnIndex == TRANSACTION_POS_GAIN && t.getType() == InvestmentTransaction.Type.SELL) {
					if (t.getGain() != null) {
						return PeanutsUtil.formatCurrency(t.getGain(), currency);
					}
					return "null";
				}
				if (columnIndex == TRANSACTION_POS_QUANTITY_SUM) {
					return PeanutsUtil.formatQuantity(t.getQuantitySum());
				}
				if (columnIndex == TRANSACTION_POS_AVG_PRICE) {
					return PeanutsUtil.formatCurrency(t.getAvgPrice(), currency);
				}
				if (columnIndex == TRANSACTION_POS_INVESTED_SUM) {
					return PeanutsUtil.formatCurrency(t.getInvestedAmount(), currency);
				}
			} else if (element instanceof InvestmentTransaction t) {
				if (columnIndex == TRANSACTION_POS_DATE) {
					return PeanutsUtil.formatDate(t.getDay());
				}
				if (columnIndex == TRANSACTION_POS_QUANTITY_SUM) {
					return PeanutsUtil.formatQuantity(t.getQuantity());
				}
				if (columnIndex == TRANSACTION_POS_PRICE) {
					return PeanutsUtil.formatCurrency(t.getPrice(), currency);
				}
				if (columnIndex == TRANSACTION_POS_GAIN) {
					return PeanutsUtil.formatCurrency(t.getAmount(), currency);
				}
			}
			return null;
		}

		@Override
		public void dispose() {
			// nothing to do
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return true;
		}

		@Override
		public void addListener(ILabelProviderListener listener) {
			// nothing to do
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
			// nothing to do
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			return null;
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			if (element instanceof InventoryEntry entry) {
				if (columnIndex == INVENTORY_POS_GAIN || columnIndex == INVENTORY_POS_GAIN_PERCENT) {
					if (entry.getMarketValue().subtract(entry.getInvestedAmount()).signum() == -1) {
						return red;
					}
				}
				if (columnIndex == INVENTORY_POS_DAY_CHANGE) {
					if (entry.getDayChange().signum() == -1) {
						return red;
					}
				}
				if (columnIndex == INVENTORY_POS_RATE) {
					if (entry.getXIRR().signum() == -1) {
						return red;
					}
				}
			} else if (element instanceof AnalyzedInvestmentTransaction t) {
				if (columnIndex == TRANSACTION_POS_GAIN && t.getType() == InvestmentTransaction.Type.SELL) {
					if (t.getGain() != null && t.getGain().signum() == -1) {
						return red;
					}
				}
			} else if (element instanceof InvestmentTransaction t) {
				if (columnIndex == TRANSACTION_POS_GAIN && t.getType() == Type.EXPENSE) {
					return red;
				}
			}
			return null;
		}
	}

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
		final Composite banner = new Composite(top, SWT.NONE);
		banner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		layout = new GridLayout();
		layout.marginHeight = 5;
		layout.marginWidth = 10;
		layout.numColumns = 13;
		banner.setLayout(layout);
		Font boldFont = Activator.getDefault().getBoldFont();

		Label l = new Label(banner, SWT.WRAP);
		l.setText("Date:");
		l.setFont(boldFont);
		final DateComposite dateChooser = new DateComposite(banner, SWT.NONE);

		l = new Label(banner, SWT.NONE);
		l.setText("Unrealized:");
		l.setFont(boldFont);
		gainingLabel = new Label(banner, SWT.NONE);

		l = new Label(banner, SWT.NONE);
		l.setText("Realized:");
		l.setFont(boldFont);
		realizedLabel = new Label(banner, SWT.NONE);

		l = new Label(banner, SWT.NONE);
		l.setFont(boldFont);
		l.setText("Market value:");
		marketValueLabel = new Label(banner, SWT.NONE);

		l = new Label(banner, SWT.NONE);
		l.setFont(boldFont);
		l.setText("Change:");
		changeLabel = new Label(banner, SWT.NONE);


		l = new Label(banner, SWT.NONE);
		l.setFont(boldFont);
		l.setText("Filter:");
		List<SecurityCategoryMapping> securityCategoryMappings = Activator.getDefault().getAccountManager().getSecurityCategoryMappings();
		final Combo filterCombo1 = new Combo(banner, SWT.DROP_DOWN | SWT.READ_ONLY);
		final Combo filterCombo2 = new Combo(banner, SWT.DROP_DOWN | SWT.READ_ONLY);
		for (SecurityCategoryMapping securityCategoryMapping : securityCategoryMappings) {
			filterCombo1.add(securityCategoryMapping.getName());
		}
		filterCombo1.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				List<SecurityCategoryMapping> securityCategoryMappings = Activator.getDefault().getAccountManager().getSecurityCategoryMappings();
				final String name = ((Combo)e.widget).getText();
				SecurityCategoryMapping securityCategoryMapping = Iterables.find(securityCategoryMappings, new Predicate<SecurityCategoryMapping>() {
					@Override
					public boolean apply(SecurityCategoryMapping input) {
						return input.getName().equals(name);
					}
				}, null);
				if (securityCategoryMapping != null) {
					filterCombo2.setItems(securityCategoryMapping.getCategories().toArray(new String[0]));
					filterCombo2.add("", 0);
					banner.layout();
					treeViewer.refresh();
				}
			}
		});
		filterCombo2.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				treeViewer.refresh();
			}
		});


		ITransactionProvider account = getTransactions();
		if (account instanceof IConfigurable configurable) {
			showAllSecurities = Boolean.parseBoolean(configurable.getConfigurationValue(SHOW_ALL_SECURITIES));
		} else {
			showAllSecurities = false;
		}

		treeViewer = new TreeViewer(top);
		treeViewer.addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof InvestmentTransaction) {
					return true;
				}
				final String name = filterCombo1.getText();
				String value = filterCombo2.getText();
				if (StringUtils.isBlank(name) || StringUtils.isBlank(value)) {
					return true;
				}
				List<SecurityCategoryMapping> securityCategoryMappings = Activator.getDefault().getAccountManager().getSecurityCategoryMappings();
				SecurityCategoryMapping securityCategoryMapping = Iterables.find(securityCategoryMappings, new Predicate<SecurityCategoryMapping>() {
					@Override
					public boolean apply(SecurityCategoryMapping input) {
						return input.getName().equals(name);
					}
				}, null);
				if (securityCategoryMapping != null) {
					Security security = ((InventoryEntry)element).getSecurity();
					return securityCategoryMapping.getSecuritiesByCategory(value).contains(security);
				}
				return false;
			}
		});
		Tree tree = treeViewer.getTree();
		ColumnViewerToolTipSupport.enableFor(treeViewer);
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tree.setFont(Activator.getDefault().getNormalFont());
		treeViewer.setContentProvider(new InventoryContentProvider());
		Color red = Activator.getDefault().getColorProvider().get(Activator.RED);
		treeViewer.setLabelProvider(new InventoryLabelProvider(red, account.getCurrency()));
		treeViewer.addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (showAllSecurities) {
					return true;
				}
				if (element instanceof InventoryEntry entry) {
					return isNotZero(entry.getQuantity());
				}
				return true;
			}
		});

		treeViewer.setComparator(securityNameComparator);

		TreeColumn col = new TreeColumn(tree, SWT.LEFT);
		col.setText("Name");
		col.setResizable(true);
		col.setWidth(170);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TreeColumn)e.widget, securityNameComparator);
			}
		});
		TreeViewerColumn treeViewerColumn = new TreeViewerColumn(treeViewer, col);
		treeViewerColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				Object element = cell.getElement();
				if (element instanceof InventoryEntry entry) {
					cell.setText(entry.getSecurity().getName());
					String icon = entry.getSecurity().getConfigurationValue("icon");
					if (StringUtils.isBlank(icon)) {
						icon = "empty";
					}
					cell.setImage(Activator.getDefault().getImage("icons/"+icon+".png"));
				} else if (element instanceof InvestmentTransaction t) {
					cell.setText(" - "+PeanutsUtil.formatDate(t.getDay()) + " ("+t.getType()+")");
				}
			}
			@Override
			public String getToolTipText(Object element) {
				if (element instanceof InventoryEntry entry) {
					return StringUtils.defaultString(entry.getSecurity().getConfigurationValue("iconText"));
				}
				return super.getToolTipText(element);
			}
		});
		treeViewer.getTree().setSortColumn(col);
		treeViewer.getTree().setSortDirection(SWT.UP);

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Fraction");
		col.setResizable(true);
		col.setWidth(80);

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Quantity");
		col.setResizable(true);
		col.setWidth(80);

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Price");
		col.setResizable(true);
		col.setWidth(80);

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Market value");
		col.setResizable(true);
		col.setWidth(80);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TreeColumn)e.widget, marketValueComparator);
			}
		});

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Avg Price");
		col.setResizable(true);
		col.setWidth(120);

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Investment Sum");
		col.setResizable(true);
		col.setWidth(100);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TreeColumn)e.widget, investmentSumComparator);
			}
		});

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Gain/Lost");
		col.setResizable(true);
		col.setWidth(80);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TreeColumn)e.widget, gainingComparator);
			}
		});

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Gain/Lost (%)");
		col.setResizable(true);
		col.setWidth(80);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TreeColumn)e.widget, gainingPercentComparator);
			}
		});

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Rate p.a.");
		col.setResizable(true);
		col.setWidth(80);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TreeColumn)e.widget, ratePerYearComparator);
			}
		});

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Change");
		col.setResizable(true);
		col.setWidth(80);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TreeColumn)e.widget, dailyChangeComparator);
			}
		});

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("YoC");
		col.setToolTipText("Future Div / Investment Sum");
		col.setResizable(true);
		col.setWidth(70);
// TODO:
//		col.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				setSorting((TreeColumn)e.widget, yocComparator);
//			}
//		});

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Future Div");
		col.setToolTipText("12 months future dividends from div tab.");
		col.setResizable(true);
		col.setWidth(70);
// TODO:
//		col.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				setSorting((TreeColumn)e.widget, yocComparator);
//			}
//		});

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Div%");
		col.setToolTipText("Based on fundamental data and latest stock price.");
		col.setResizable(true);
		col.setWidth(70);
// TODO:
//		col.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				setSorting((TreeColumn)e.widget, yocComparator);
//			}
//		});

		new PersistentColumWidth(tree, Activator.getDefault().getPreferenceStore(), 
				getClass().getCanonicalName()+"."+getEditorInput().getName());
		
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection sel = (IStructuredSelection)event.getSelection();
				if (sel.getFirstElement() instanceof InventoryEntry invEntry) {
					Security security = invEntry.getSecurity();
					IEditorInput input = new SecurityEditorInput(security);
					try {
						getSite().getWorkbenchWindow().getActivePage().openEditor(input,
							SecurityEditor.ID);
					} catch (PartInitException e) {
						e.printStackTrace();
					}
				}
			}
		});

		priceProviderFactory = PriceProviderFactory.getInstance(account.getCurrency(), Activator.getDefault().getExchangeRates());
		inventory = new Inventory(account, priceProviderFactory, new AnalyzerFactory(), Activator.getDefault().getAccountManager());
		inventory.setDate(date);
		inventory.addPropertyChangeListener(inventoryChangeListener);
//		dividendStats = new DividendStats(Activator.getDefault().getAccountManager(), priceProviderFactory,
//				Activator.getDefault().getAccountManager());
		
		treeViewer.setInput(inventory);

		dateChooser.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				date = dateChooser.getDay();
				inventory.setDate(date);
				updateAll();
			}
		});

		MenuManager menuMgr = new MenuManager("#popupMenu", AccountEditor.ID); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager menuManager) {
				menuManager.add(new Separator("top")); //$NON-NLS-1$
				menuManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
				menuManager.add(new Separator());
				menuManager.add(new Action("Show all securities", IAction.AS_CHECK_BOX) {
					@Override
					public void run() {
						showAllSecurities = ! showAllSecurities;
						ITransactionProvider transactionProvider = getTransactions();
						if (transactionProvider instanceof IConfigurable configurable) {
							configurable.putConfigurationValue(SHOW_ALL_SECURITIES, Boolean.valueOf(showAllSecurities).toString());
						}
						treeViewer.refresh();
					}
					@Override
					public boolean isChecked() {
						return showAllSecurities;
					}
				});
			}
		});
		tree.setMenu(menuMgr.createContextMenu(tree));
		getSite().setSelectionProvider(treeViewer);
		getSite().registerContextMenu(menuMgr, getSite().getSelectionProvider());
		updateAll();
	}
	
	protected void updateAll() {
		ITransactionProvider account = getTransactions();
		treeViewer.refresh(true);
		gainingLabel.setText(PeanutsUtil.formatCurrency(inventory.getUnrealizedGainings(), account.getCurrency()));
		RealizedGain realizedGain = new RealizedGain(inventory);
		BigDecimal realized = realizedGain.gain(date.year);
		realizedLabel.setText(PeanutsUtil.formatCurrency(realized, account.getCurrency()));
		marketValueLabel.setText(PeanutsUtil.formatCurrency(inventory.getMarketValue(), account.getCurrency()));
		changeLabel.setText(PeanutsUtil.formatCurrency(inventory.getDayChange(), account.getCurrency()));
		marketValueLabel.getParent().layout();
	}

	private ITransactionProvider getTransactions() {
		return ((ITransactionProviderInput) getEditorInput()).getTransactionProvider();
	}

	@Override
	public void dispose() {
		inventory.removePropertyChangeListener(inventoryChangeListener);
		inventory.dispose();
//		dividendStats.dispose();
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
		treeViewer.getTree().setFocus();
	}

	protected void setSorting(TreeColumn column, InventoryComparator newComparator) {
		Tree tree2 = treeViewer.getTree();
		if (treeViewer.getComparator() == newComparator) {
			int currentSortDirection = tree2.getSortDirection();
			tree2.setSortDirection(currentSortDirection==SWT.UP?SWT.DOWN:SWT.UP);
			treeViewer.refresh();
		} else {
			tree2.setSortColumn(column);
			tree2.setSortDirection(SWT.UP);
			treeViewer.setComparator(newComparator);
		}
	}

}
