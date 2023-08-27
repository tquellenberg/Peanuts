package de.tomsplayground.peanuts.client.editors.security;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import de.tomsplayground.peanuts.app.marketscreener.MarketScreener;
import de.tomsplayground.peanuts.app.yahoo.MarketCap;
import de.tomsplayground.peanuts.app.yahoo.YahooAPI;
import de.tomsplayground.peanuts.app.yahoo.YahooAPI.DebtEquityValue;
import de.tomsplayground.peanuts.app.yahoo.YahooAPI.YahooData;
import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.editors.security.properties.SecurityPropertyPage;
import de.tomsplayground.peanuts.client.util.UniqueAsyncExecution;
import de.tomsplayground.peanuts.client.widgets.CurrencyComboViewer;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.base.InventoryEntry;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.currenncy.Currencies;
import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;
import de.tomsplayground.peanuts.domain.currenncy.ExchangeRates;
import de.tomsplayground.peanuts.domain.fundamental.AvgFundamentalData;
import de.tomsplayground.peanuts.domain.fundamental.CurrencyAjustedFundamentalData;
import de.tomsplayground.peanuts.domain.fundamental.FundamentalData;
import de.tomsplayground.peanuts.domain.fundamental.FundamentalDatas;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.domain.process.StockSplit;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class FundamentalDataEditorPart extends EditorPart {

	public static final String SECURITY_MARKET_CAP_VALUE = "security.marketCap.value";
	public static final String SECURITY_MARKET_CAP_CURRENCY = "security.marketCap.currency";

	private static final BigDecimal DEPT_LIMIT = new BigDecimal("1.0");
	private static final BigDecimal DIV_LIMIT = new BigDecimal("0.03");
	private static final BigDecimal EPS_GROWTH_LIMIT_UP = new BigDecimal("0.07");
	private static final BigDecimal EPS_GROWTH_LIMIT_DOWN = new BigDecimal("0.02");
	private static final BigDecimal DIVIDEND_LIMIT = new BigDecimal("0.9");
	private static final BigDecimal DIVIDEND_GROWTH_UP = new BigDecimal("0.08");
	private static final BigDecimal DIVIDEND_GROWTH_DOWN = new BigDecimal("0.02");

	private TableViewer tableViewer;
	private final int colWidth[] = new int[15];
	private boolean dirty = false;
	private List<FundamentalData> fundamentalDataList;
	private IPriceProvider priceProvider;
	private InventoryEntry inventoryEntry;
	private CurrencyComboViewer currencyComboViewer;
	private CurrencyConverter currencyConverter;
	private List<Object> tableRows;

	private Button deYahooGo;

	private class FundamentalDataTableLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == 0) {
				if (element instanceof FundamentalData data) {
					if (data.isLocked()) {
						return Activator.getDefault().getImage("icons/lock.png");
					}
				}
			}
			return null;
		}

		private FundamentalData getPreviousYear(FundamentalData data) {
			final int prevYear = data.getYear() -1;
			return Iterables.find(fundamentalDataList, new Predicate<FundamentalData>() {
				@Override
				public boolean apply(FundamentalData arg0) {
					return arg0.getYear() == prevYear;
				}
			}, null);
		}

		private BigDecimal growth(BigDecimal now, BigDecimal prev) {
			if (prev.signum() == -1 && now.signum() == 1) {
				return BigDecimal.ZERO;
			}
			if (prev.signum() != 0) {
				return now.subtract(prev).divide(prev.abs(), PeanutsUtil.MC);
			}
			return BigDecimal.ZERO;
		}

		private BigDecimal epsGrowth(FundamentalData data) {
			FundamentalData previousYearData = getPreviousYear(data);
			if (previousYearData != null) {
				BigDecimal eps = data.getEarningsPerShare();
				BigDecimal prevEps = previousYearData.getEarningsPerShare();
				return growth(eps, prevEps);
			}
			return BigDecimal.ZERO;
		}

		private BigDecimal divGrowth(FundamentalData data) {
			FundamentalData previousYearData = getPreviousYear(data);
			if (previousYearData != null) {
				BigDecimal div = data.getDividende();
				BigDecimal prevDiv = previousYearData.getDividende();
				return growth(div, prevDiv);
			}
			return BigDecimal.ZERO;
		}

		private BigDecimal adjustedEpsGrowth(FundamentalData data) {
			FundamentalData previousYearData = getPreviousYear(data);
			if (previousYearData != null) {
				BigDecimal eps = currencyAdjustedEPS(data);
				BigDecimal prevEps = currencyAdjustedEPS(previousYearData);
				return growth(eps, prevEps);
			}
			return BigDecimal.ZERO;
		}

		private BigDecimal adjustedDivGrowth(FundamentalData data) {
			FundamentalData previousYearData = getPreviousYear(data);
			if (previousYearData != null) {
				BigDecimal div = currencyAdjustedDiv(data);
				BigDecimal prevDiv = currencyAdjustedDiv(previousYearData);
				if (prevDiv.signum() != 0) {
					return div.divide(prevDiv, PeanutsUtil.MC).subtract(BigDecimal.ONE);
				}
			}
			return BigDecimal.ZERO;
		}

		private BigDecimal currencyAdjustedEPS(FundamentalData data) {
			CurrencyAjustedFundamentalData currencyAjustedData = new CurrencyAjustedFundamentalData(data, currencyConverter);
			return currencyAjustedData.getEarningsPerShare();
		}

		private BigDecimal currencyAdjustedDiv(FundamentalData data) {
			CurrencyAjustedFundamentalData currencyAjustedData = new CurrencyAjustedFundamentalData(data, currencyConverter);
			return currencyAjustedData.getDividende();
		}

		private BigDecimal peRatio(FundamentalData data) {
			CurrencyAjustedFundamentalData currencyAjustedData = new CurrencyAjustedFundamentalData(data, currencyConverter);
			return currencyAjustedData.calculatePeRatio(priceProvider);
		}

		private BigDecimal divYield(FundamentalData data) {
			CurrencyAjustedFundamentalData currencyAjustedData = new CurrencyAjustedFundamentalData(data, currencyConverter);
			return currencyAjustedData.calculateDivYield(priceProvider);
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof AvgFundamentalData) {
				// Fresh data
				FundamentalDatas fundamentalDatas = new FundamentalDatas(fundamentalDataList, getSecurity());
				AvgFundamentalData data = fundamentalDatas.getAvgFundamentalData(priceProvider, Activator.getDefault().getExchangeRates());
				switch (columnIndex) {
					case 0:
						return "Avg";
					case 3:
						BigDecimal avgDividendGrowth = data.getAvgDividendGrowth();
						if (avgDividendGrowth == null) {
							return "";
						}
						return PeanutsUtil.formatPercent(avgDividendGrowth.subtract(BigDecimal.ONE));
					case 5:
						BigDecimal avgCurrencyAdjustedDividendGrowth = data.getAvgCurrencyAdjustedDividendGrowth();
						if (avgCurrencyAdjustedDividendGrowth == null) {
							return "";
						}
						return PeanutsUtil.formatPercent(avgCurrencyAdjustedDividendGrowth.subtract(BigDecimal.ONE));
					case 7:
						BigDecimal avgEpsGrowth = data.getAvgEpsGrowth();
						if (avgEpsGrowth == null) {
							return "";
						}
						return PeanutsUtil.formatPercent(avgEpsGrowth.subtract(BigDecimal.ONE));
					case 9:
						BigDecimal currencyAdjustedAvgEpsGrowth = data.getCurrencyAdjustedAvgEpsGrowth();
						if (currencyAdjustedAvgEpsGrowth == null) {
							return "";
						}
						return PeanutsUtil.formatPercent(currencyAdjustedAvgEpsGrowth.subtract(BigDecimal.ONE));
					case 11:
						String avgPe = PeanutsUtil.format(data.getAvgPE(), 1);
						String overriddenAvgPE = getSecurity().getConfigurationValue(FundamentalDatas.OVERRIDDEN_AVG_PE);
						if (StringUtils.isNotBlank(overriddenAvgPE)) {
							avgPe = avgPe + " ("+overriddenAvgPE+")";
						}
						return avgPe;
					default:
						return "";
				}
			} else if (element instanceof FundamentalData data) {
				switch (columnIndex) {
					case 0:
						return String.valueOf(data.getYear());
					case 1:
						return String.valueOf(data.getFicalYearEndsMonth());
					case 2:
						return PeanutsUtil.formatCurrency(data.getDividende(), null);
					case 3:
						return PeanutsUtil.formatPercent(divGrowth(data));
					case 4:
						return PeanutsUtil.formatCurrency(currencyAdjustedDiv(data), null);
					case 5:
						return PeanutsUtil.formatPercent(adjustedDivGrowth(data));
					case 6:
						return PeanutsUtil.formatCurrency(data.getEarningsPerShare(), null);
					case 7:
						return PeanutsUtil.formatPercent(epsGrowth(data));
					case 8:
						return PeanutsUtil.formatCurrency(currencyAdjustedEPS(data), null);
					case 9:
						return PeanutsUtil.formatPercent(adjustedEpsGrowth(data));
					case 10:
						return PeanutsUtil.format(data.getDebtEquityRatio(), 2);
					case 11:
						return PeanutsUtil.format(peRatio(data), 1);
					case 12:
						return PeanutsUtil.formatPercent(divYield(data));
					case 13:
						LocalDateTime lastModifyDate = data.getLastModifyDate();
						if (lastModifyDate != null) {
							return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(lastModifyDate);
						}
						return "";
					default:
						return "";
				}
			} else {
				return "";
			}
		}
		@Override
		public String getText(Object element) {
			if (element instanceof FundamentalData data) {
				return String.valueOf(data.getYear());
			}
			return "";
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			if (element instanceof AvgFundamentalData) {
				FundamentalDatas fundamentalDatas = new FundamentalDatas(fundamentalDataList, getSecurity());
				AvgFundamentalData data = fundamentalDatas.getAvgFundamentalData(priceProvider, Activator.getDefault().getExchangeRates());
				if (columnIndex == 3 || columnIndex == 5) {
					BigDecimal avgDividendGrowth;
					if (columnIndex == 3) {
						avgDividendGrowth = data.getAvgDividendGrowth();
					} else {
						avgDividendGrowth = data.getAvgCurrencyAdjustedDividendGrowth();
					}
					if (avgDividendGrowth != null) {
						if (avgDividendGrowth.subtract(BigDecimal.ONE).compareTo(DIVIDEND_GROWTH_UP) >= 0) {
							return Activator.getDefault().getColorProvider().get(Activator.GREEN_BG);
						}
						if (avgDividendGrowth.subtract(BigDecimal.ONE).compareTo(DIVIDEND_GROWTH_DOWN) <= 0) {
							return Activator.getDefault().getColorProvider().get(Activator.RED_BG);
						}
					}					
				}
				if (columnIndex == 7 || columnIndex == 9) {
					BigDecimal avgEpsGrowth;
					if (columnIndex == 7) {
						avgEpsGrowth = data.getAvgEpsGrowth();
					} else {
						avgEpsGrowth = data.getCurrencyAdjustedAvgEpsGrowth();
					}
					if (avgEpsGrowth != null) {
						if (avgEpsGrowth.subtract(BigDecimal.ONE).compareTo(EPS_GROWTH_LIMIT_UP) >= 0) {
							return Activator.getDefault().getColorProvider().get(Activator.GREEN_BG);
						}
						if (avgEpsGrowth.subtract(BigDecimal.ONE).compareTo(EPS_GROWTH_LIMIT_DOWN) <= 0) {
							return Activator.getDefault().getColorProvider().get(Activator.RED_BG);
						}
					}
				}
			} else if (element instanceof FundamentalData data) {
				if (data.isIgnoreInAvgCalculation()) {
					return Activator.getDefault().getColorProvider().get(Activator.INACTIVE_ROW);
				} else if (columnIndex == 2) {
					BigDecimal dividende = data.getDividende();
					BigDecimal earningsPerShare = data.getEarningsPerShare();
					if (dividende.compareTo(BigDecimal.ZERO) > 0) {
						if (earningsPerShare.signum() <= 0) {
							return Activator.getDefault().getColorProvider().get(Activator.RED_BG);
						} else {
							BigDecimal ratio = dividende.divide(earningsPerShare, PeanutsUtil.MC);
							if (ratio.compareTo(DIVIDEND_LIMIT) > 0) {
								return Activator.getDefault().getColorProvider().get(Activator.RED_BG);
							}
						}
					}
				} else if (columnIndex == 6) {
					BigDecimal earningsPerShare = data.getEarningsPerShare();
					if (earningsPerShare.signum() < 0) {
						return Activator.getDefault().getColorProvider().get(Activator.RED_BG);
					}
				} else if (columnIndex == 10) {
					BigDecimal debtEquityRatio = data.getDebtEquityRatio();
					if (debtEquityRatio.compareTo(DEPT_LIMIT) >= 0 || debtEquityRatio.signum() == -1) {
						return Activator.getDefault().getColorProvider().get(Activator.RED_BG);
					}
				} else if (columnIndex == 12) {
					BigDecimal divYield = divYield(data);
					if (divYield.compareTo(DIV_LIMIT) >= 0) {
						return Activator.getDefault().getColorProvider().get(Activator.GREEN_BG);
					}
				}
			}
			return null;
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			if (element instanceof FundamentalData data) {
				if (columnIndex == 3) {
					if (divGrowth(data).signum() == -1) {
						return Activator.getDefault().getColorProvider().get(Activator.RED);
					}
				}
				if (columnIndex == 5) {
					if (adjustedDivGrowth(data).signum() == -1) {
						return Activator.getDefault().getColorProvider().get(Activator.RED);
					}
				}
				if (columnIndex == 7) {
					if (epsGrowth(data).signum() == -1) {
						return Activator.getDefault().getColorProvider().get(Activator.RED);
					}
				}
				if (columnIndex == 9) {
					if (adjustedEpsGrowth(data).signum() == -1) {
						return Activator.getDefault().getColorProvider().get(Activator.RED);
					}
				}
				FundamentalData currentFundamentalData = getSecurity().getFundamentalDatas().getCurrentFundamentalData();
				if (currentFundamentalData != null && currentFundamentalData.getYear() == data.getYear()) {
					return Activator.getDefault().getColorProvider().get(Activator.ACTIVE_ROW);
				}
			}
			return null;
		}
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (!(input instanceof SecurityEditorInput)) {
			throw new PartInitException("Invalid Input: Must be SecurityEditorInput");
		}
		setSite(site);
		setInput(input);
		setPartName(input.getName());
	}

	@Override
	public void dispose() {
		getSecurity().removePropertyChangeListener(securityPropertyChangeListener);
		super.dispose();
	}

	@Override
	public void createPartControl(Composite parent) {
		final Security security = getSecurity();

		Composite top = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		top.setLayout(layout);

		Composite metaComposite = new Composite(top, SWT.NONE);
		metaComposite.setLayout(new GridLayout(7, false));
		currencyComboViewer = new CurrencyComboViewer(metaComposite, false, false);
		new Label(metaComposite, SWT.NONE).setText("Morningstar symbol:");
		final Text morningstarSymbol = new Text(metaComposite, SWT.NONE);
		morningstarSymbol.setText(security.getMorningstarSymbol());
		morningstarSymbol.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				security.setMorningstarSymbol(morningstarSymbol.getText());
				markDirty();
			}
		});
		GridData layoutData = new GridData();
		layoutData.widthHint = 100;
		morningstarSymbol.setLayoutData(layoutData);

		fourTradersGo = new Button(metaComposite, SWT.PUSH);
		fourTradersGo.setText(is4Traders(security)?"Load data from 4-Traders":"Load data from MarketScreener");
		fourTradersGo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateMarketScreenerData(security);
			}
		});
		fourTradersGo.setEnabled(StringUtils.isNotBlank(security.getConfigurationValue(SecurityPropertyPage.MARKET_SCREENER_URL)));

		deYahooGo = new Button(metaComposite, SWT.PUSH);
		deYahooGo.setText("Load D/E from Yahoo");
		deYahooGo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateDeYahooData(security);
			}
		});

		marketCapLable = new Label(metaComposite, SWT.NONE);
		updateMarketCapLable();

		updateButtonState();
		getSecurity().addPropertyChangeListener(securityPropertyChangeListener);

		tableViewer = new TableViewer(top, SWT.FULL_SELECTION | SWT.MULTI);
		Table table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setFont(Activator.getDefault().getNormalFont());

		int colNumber = 0;
		TableColumn col = new TableColumn(table, SWT.LEFT);
		col.setText("Year");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 80);
		col.setResizable(true);
		ViewerComparator comparator = new ViewerComparator() {
			@Override
			public int category(Object element) {
				if (element instanceof AvgFundamentalData) {
					return 99;
				}
				return 0;
			}
			@Override
			public boolean isSorterProperty(Object element, String property) {
				return "year".equals(property);
			}
		};
		tableViewer.setComparator(comparator);
		table.setSortColumn(col);
		table.setSortDirection(SWT.UP);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Fiscal Year");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 40);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Dividende");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 70);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Change %");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 70);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Dividende "+security.getCurrency().getSymbol());
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 70);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Change %");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 70);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("EPS");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Change %");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 70);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("EPS "+security.getCurrency().getSymbol());
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 70);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Change %");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 70);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("D/E ratio");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("P/E ratio");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 70);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Div%");
		col.setToolTipText("Based on fundamental data and latest stock price.");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 70);
		col.setResizable(true);
		colNumber++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Date");
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 120);
		col.setResizable(true);
		colNumber++;

		tableViewer.setColumnProperties(new String[] { "year", "fiscalYear", "div", "divgr", "div2", "div2gr",
			"EPS", "EPSgr", "EPS2", "EPS2gr", "deRatio", "peRatio", "divYield", "date"});
		tableViewer.setCellModifier(new ICellModifier() {

			@Override
			public boolean canModify(Object element, String property) {
				return (element instanceof FundamentalData fData) && (! fData.isLocked()) &&
					List.of("year", "fiscalYear", "div", "EPS", "deRatio").contains(property);
			}

			@Override
			public Object getValue(Object element, String property) {
				FundamentalData p = (FundamentalData) element;
				if (property.equals("year")) {
					return String.valueOf(p.getYear());
				} else if (property.equals("fiscalYear")) {
					return String.valueOf(p.getFicalYearEndsMonth());
				} else if (property.equals("div")) {
					return PeanutsUtil.formatCurrency(p.getDividende(), null);
				} else if (property.equals("EPS")) {
					return PeanutsUtil.formatCurrency(p.getEarningsPerShare(), null);
				} else if (property.equals("deRatio")) {
					return PeanutsUtil.formatCurrency(p.getDebtEquityRatio(), null);
				}
				return null;
			}

			@Override
			public void modify(Object element, String property, Object value) {
				FundamentalData p = (FundamentalData) ((TableItem) element).getData();
				try {
					boolean changed = false;
					if (property.equals("year")) {
						Integer newYear = Integer.valueOf((String) value);
						if (newYear.intValue() != p.getYear()) {
							p.setYear(newYear.intValue());
							changed = true;
						}
					} else if (property.equals("fiscalYear")) {
						Integer newFiscalYear = Integer.valueOf((String) value);
						if (newFiscalYear.intValue() != p.getFicalYearEndsMonth()) {
							p.setFicalYearEndsMonth(newFiscalYear.intValue());
							changed = true;
						}
					} else if (property.equals("div")) {
						BigDecimal v = PeanutsUtil.parseCurrency((String) value);
						if (v.compareTo(p.getDividende()) != 0) {
							p.setDividende(v);
							changed = true;
						}
					} else if (property.equals("EPS")) {
						BigDecimal v = PeanutsUtil.parseCurrency((String) value);
						if (v.compareTo(p.getEarningsPerShare()) != 0) {
							p.setEarningsPerShare(v);
							changed = true;
						}
					} else if (property.equals("deRatio")) {
						BigDecimal v = PeanutsUtil.parseCurrency((String) value);
						if (v.compareTo(p.getDebtEquityRatio()) != 0) {
							p.setDebtEquityRatio(v);
							changed = true;
						}
					}
					if (changed) {
						p.updateLastModifyDate();
						tableViewer.refresh();
						markDirty();
					}
				} catch (ParseException | NumberFormatException e) {
					// Okay
				}
			}
		});
		tableViewer.setCellEditors(new CellEditor[] {new TextCellEditor(table), new TextCellEditor(table), new TextCellEditor(table),
			new TextCellEditor(table), new TextCellEditor(table), new TextCellEditor(table), new TextCellEditor(table),
			new TextCellEditor(table), new TextCellEditor(table), new TextCellEditor(table), new TextCellEditor(table),
			new TextCellEditor(table)});

		tableViewer.setLabelProvider(new FundamentalDataTableLabelProvider());
		tableViewer.setContentProvider(new ArrayContentProvider());
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		ImmutableList<StockSplit> stockSplits = Activator.getDefault().getAccountManager().getStockSplits(security);
		priceProvider = PriceProviderFactory.getInstance().getSplitAdjustedPriceProvider(security, stockSplits);

		Inventory inventory = Activator.getDefault().getAccountManager().getFullInventory();
		for (InventoryEntry entry : inventory.getEntries()) {
			if (entry.getSecurity().equals(security)) {
				inventoryEntry = entry;
			}
		}

		ExchangeRates exchangeRates = Activator.getDefault().getExchangeRates();
		FundamentalDatas fundamentalDatas = security.getFundamentalDatas();
		fundamentalDataList = cloneFundamentalData(fundamentalDatas.getDatas());
		if (! fundamentalDataList.isEmpty()) {
			Currency currency = fundamentalDatas.getCurrency();
			currencyConverter = exchangeRates.createCurrencyConverter(currency, security.getCurrency());
		} else {
			currencyConverter = exchangeRates.createCurrencyConverter(security.getCurrency(), security.getCurrency());
		}
		currencyComboViewer.selectCurrency(currencyConverter.getFromCurrency());

		tableRows = new ArrayList<Object>();
		tableRows.addAll(fundamentalDataList);
		tableRows.add(fundamentalDatas.getAvgFundamentalData(priceProvider, exchangeRates));
		tableViewer.setInput(tableRows);

		currencyComboViewer.getCombo().addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Currency selectedCurrency = currencyComboViewer.getSelectedCurrency();
				ExchangeRates exchangeRates = Activator.getDefault().getExchangeRates();
				currencyConverter = exchangeRates.createCurrencyConverter(selectedCurrency, security.getCurrency());
				updateFundamentalData(selectedCurrency);				
				tableViewer.refresh(true);
				markDirty();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				markDirty();
			}
		});

		MenuManager menuManager = new MenuManager();
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});
		table.setMenu(menuManager.createContextMenu(table));
		getSite().registerContextMenu(menuManager, tableViewer);
		getSite().setSelectionProvider(tableViewer);
	}

	private final PropertyChangeListener securityPropertyChangeListener = new UniqueAsyncExecution() {
		@Override
		public void doit(PropertyChangeEvent evt, Display display) {
			if (evt.getPropertyName().equals(Security.CONFIG_KEY_YAHOO_SYMBOL)) {
				updateButtonState();
			}
			if (evt.getPropertyName().equals(FundamentalDatas.OVERRIDDEN_AVG_PE) ||
				evt.getPropertyName().equals("fundamentalData")) {
				if (! tableViewer.getTable().isDisposed()) {
					tableViewer.refresh(true);
				}
			}
		}
		@Override
		public Display getDisplay() {
			return getSite().getShell().getDisplay();
		}
	};

	private void updateFundamentalData(Currency selectedCurrency) {
		for (FundamentalData d : fundamentalDataList) {
			d.setCurrency(selectedCurrency);
		}
	}
	
	private Button fourTradersGo;
	private Label marketCapLable;

	private void updateButtonState() {
		Security security = getSecurity();
		deYahooGo.setEnabled(StringUtils.isNotBlank(security.getConfigurationValue(Security.CONFIG_KEY_YAHOO_SYMBOL)));
		fourTradersGo.setEnabled(StringUtils.isNotBlank(security.getConfigurationValue(SecurityPropertyPage.MARKET_SCREENER_URL)));
	}

	private void updateMarketCapLable() {
		Security security = getSecurity();
		String currency = security.getConfigurationValue(SECURITY_MARKET_CAP_CURRENCY);
		String marketCapValueStr = security.getConfigurationValue(SECURITY_MARKET_CAP_VALUE);

		if (StringUtils.isNotEmpty(currency) && StringUtils.isNotEmpty(marketCapValueStr)) {
			MarketCap marketCap = new MarketCap(new BigDecimal(marketCapValueStr), currency);
			String labelText = "Market Cap: " + PeanutsUtil.formatHugeNumber(marketCap.getMarketCap())
				+ " " + marketCap.getCurrency().getSymbol();
			try {
				Currency defaultCurrency = Currencies.getInstance().getDefaultCurrency();
				if (! marketCap.getCurrency().equals(defaultCurrency)) {
					BigDecimal valueDefaultCur = marketCap.getMarketCapInDefaultCurrency(Activator.getDefault().getExchangeRates());
					labelText += " - " + PeanutsUtil.formatHugeNumber(valueDefaultCur) + " " + defaultCurrency.getSymbol();
				}
			} catch (IllegalArgumentException e) {
				System.err.println("Unknown currency: "+currency);
				labelText += " " + currency;
			}
			marketCapLable.setText(labelText);
			marketCapLable.getParent().pack(true);
		}
	}

	private void updateDeYahooData(final Security security) {
		String symbol = security.getConfigurationValue(Security.CONFIG_KEY_YAHOO_SYMBOL);
		try {
			String apiKey = Activator.getDefault().getPreferenceStore().getString(Activator.RAPIDAPIKEY_PROPERTY);
			YahooData yahooData = new YahooAPI(apiKey).getYahooData(symbol);
			if (yahooData == null) {
				MessageDialog.openError(getSite().getShell(), "Error", "Error loading data from Yahoo API");
				return;
			}
			MarketCap marketCap = yahooData.getMarketCap();
			if (marketCap != null) {
				security.putConfigurationValue(SECURITY_MARKET_CAP_CURRENCY, Objects.toString(marketCap.getCurrency(), ""));
				security.putConfigurationValue(SECURITY_MARKET_CAP_VALUE, marketCap.getMarketCap().toPlainString());
				updateMarketCapLable();
			}
			boolean valueChanged = false;
			for (DebtEquityValue debtEquityValue : yahooData.getDebtEquityValue()) {
				for (FundamentalData oldData : fundamentalDataList) {
					if (oldData.isIncluded(debtEquityValue.getDay())) {
						BigDecimal newValue = new BigDecimal(debtEquityValue.getValue());
						BigDecimal oldValue = oldData.getDebtEquityRatio();
						if (oldValue != null && oldValue.compareTo(newValue) != 0) {
							oldData.setDebtEquityRatio(newValue);
							oldData.updateLastModifyDate();
							valueChanged = true;
						}
						break;
					}
				}
			}
			if (valueChanged) {
				markDirty();
				tableViewer.refresh(true);
			}
		} catch (RuntimeException ex) {
			ex.printStackTrace();
		}
	}

	private boolean is4Traders(Security security) {
		String financialsUrl = security.getConfigurationValue(SecurityPropertyPage.MARKET_SCREENER_URL);
		return StringUtils.isNotBlank(financialsUrl) && financialsUrl.contains("4-traders.com");
	}
	
	private void updateMarketScreenerData(final Security security) {
		try {
			String financialsUrl = security.getConfigurationValue(SecurityPropertyPage.MARKET_SCREENER_URL);
			if (StringUtils.isNotBlank(financialsUrl)) {
				MarketScreener marketScreener = new MarketScreener();
				updateFundamentaData(marketScreener.scrapFinancials(financialsUrl));
			} else {
				String errorText = "No unique result could be found for "+security.getISIN();
				IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, errorText);
				ErrorDialog.openError(getSite().getShell(), errorText, null, status);
			}
		} catch (RuntimeException ex) {
			ex.printStackTrace();
		}
	}

	private void updateFundamentaData(List<FundamentalData> newDatas) {
		for (FundamentalData newData : newDatas) {
			newData.setCurrency(currencyConverter.getFromCurrency());
			if (newData.getDividende().signum() == 0 && newData.getEarningsPerShare().signum() == 0) {
				continue;
			}
			boolean dataExists = false;
			for (FundamentalData existingData : fundamentalDataList) {
				if (newData.getYear() == existingData.getYear()) {
					// Not older than 6 months
					if (existingData.getFiscalEndDay().delta(Day.today()) <= 180) {
						existingData.update(newData);
					}
					dataExists = true;
					break;
				}
			}
			if (! dataExists) {
				fundamentalDataList.add(newData);
				tableRows.add(newData);
			}
		}
		markDirty();
		tableViewer.refresh(true);
	}

	private List<FundamentalData> cloneFundamentalData(Collection<FundamentalData> datas) {
		List<FundamentalData> fundamentalDatas = new ArrayList<FundamentalData>();
		for (FundamentalData d : datas) {
			fundamentalDatas.add(new FundamentalData(d));
		}
		Collections.sort(fundamentalDatas);
		return fundamentalDatas;
	}

	@Override
	public void setFocus() {
		tableViewer.getTable().setFocus();
	}

	protected void fillContextMenu(IMenuManager manager) {
		manager.add(new Action("New") {
			@Override
			public void run() {
				FundamentalData fundamentalData = new FundamentalData();
				fundamentalData.setCurrency(currencyComboViewer.getSelectedCurrency());
				fundamentalDataList.add(fundamentalData);
				tableRows.add(fundamentalData);
				tableViewer.add(fundamentalData);
				tableViewer.getTable().redraw();
				markDirty();
			}
		});
	}

	public void deleteFundamentalData(Collection<FundamentalData> data) {
		if (fundamentalDataList.removeAll(data)) {
			tableRows.removeAll(data);
			tableViewer.remove(data.toArray());
			markDirty();
		}
	}

	public void ignoreFundamentalData(Collection<FundamentalData> data) {
		for (FundamentalData fundamentalData : data) {
			fundamentalData.setIgnoreInAvgCalculation(! fundamentalData.isIgnoreInAvgCalculation());
			tableViewer.refresh();
			markDirty();
		}
	}

	public void lockFundamentalData(Collection<FundamentalData> data) {
		for (FundamentalData fundamentalData : data) {
			fundamentalData.setLocked(! fundamentalData.isLocked());
			tableViewer.refresh();
			markDirty();
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		Security security = getSecurity();
		List<FundamentalData> datas = cloneFundamentalData(fundamentalDataList);
		Currency selectedCurrency = currencyComboViewer.getSelectedCurrency();
		for (FundamentalData fundamentalData : datas) {
			fundamentalData.setCurrency(selectedCurrency);
		}
		security.setFundamentalDatas(datas);
		dirty = false;
	}

	private Security getSecurity() {
		return ((SecurityEditorInput) getEditorInput()).getSecurity();
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	public void markDirty() {
		dirty = true;
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}

}
