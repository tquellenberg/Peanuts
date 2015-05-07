package de.tomsplayground.peanuts.client.editors.account;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
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
import org.eclipse.jface.resource.JFaceResources;
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
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.editors.ITransactionProviderInput;
import de.tomsplayground.peanuts.client.editors.security.SecurityEditor;
import de.tomsplayground.peanuts.client.editors.security.SecurityEditorInput;
import de.tomsplayground.peanuts.client.util.UniqueAsyncExecution;
import de.tomsplayground.peanuts.client.widgets.DateComposite;
import de.tomsplayground.peanuts.config.IConfigurable;
import de.tomsplayground.peanuts.domain.base.ITransactionProvider;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.base.InventoryEntry;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction.Type;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.domain.reporting.investment.AnalyzedInvestmentTransaction;
import de.tomsplayground.peanuts.domain.reporting.investment.AnalyzerFactory;
import de.tomsplayground.peanuts.util.PeanutsUtil;
import de.tomsplayground.util.Day;

public class InventoryEditorPart extends EditorPart {

	private static final String SHOW_ALL_SECURITIES = "inventoryShowAllSecurities";
	private final int colWidth[] = new int[11];
	private TreeViewer treeViewer;
	private Label gainingLabel;
	private Label marketValueLabel;
	private Label changeLabel;
	private Day date = new Day();

	private boolean showAllSecurities;
	private Inventory inventory;

	abstract private class InventoryComparator extends ViewerComparator {

		abstract int compareInventoryEntry(Viewer viewer, InventoryEntry e1, InventoryEntry e2);

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			int sort = ((TreeViewer)viewer).getTree().getSortDirection();
			if (e1 instanceof InventoryEntry && e2 instanceof InventoryEntry) {
				int compare = compareInventoryEntry(viewer, (InventoryEntry)e1, (InventoryEntry)e2);
				return (sort == SWT.DOWN) ? compare : -compare;
			}
			if (e1 instanceof InvestmentTransaction && e2 instanceof InvestmentTransaction) {
				InvestmentTransaction i1 = (InvestmentTransaction)e1;
				InvestmentTransaction i2 = (InvestmentTransaction)e2;
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
			return i1.getMarketValue(date).compareTo(i2.getMarketValue(date));
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
			return entry.getMarketValue(date).subtract(entry.getInvestedAmount());
		}
		@Override
		public int compareInventoryEntry(Viewer viewer, InventoryEntry i1, InventoryEntry i2) {
			return gaining(i1).compareTo(gaining(i2));
		}
	};

	private final InventoryComparator gainingPercentComparator = new InventoryComparator() {
		private BigDecimal gainingPercent(InventoryEntry entry) {
			if (entry.getInvestedAmount().compareTo(BigDecimal.ZERO) != 0) {
				BigDecimal gain = entry.getMarketValue(date).subtract(entry.getInvestedAmount());
				return gain.divide(entry.getInvestedAmount(), new MathContext(10, RoundingMode.HALF_EVEN));
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
			return i1.getXIRR(date).compareTo(i2.getXIRR(date));
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

	private static class InventoryContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof InventoryEntry) {
				InventoryEntry entry = (InventoryEntry) parentElement;
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
		public boolean hasChildren(Object element) {
			return element instanceof InventoryEntry;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			Inventory inventory = (Inventory) inputElement;
			List<InventoryEntry> entries = new ArrayList<InventoryEntry>(inventory.getEntries());
			return entries.toArray();
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
			if (element instanceof InventoryEntry) {
				InventoryEntry entry = (InventoryEntry) element;
				if (columnIndex == INVENTORY_FRACTION) {
					if (inventory.getMarketValue().compareTo(BigDecimal.ZERO) != 0) {
						BigDecimal fraction = entry.getMarketValue(date).divide(inventory.getMarketValue(), new MathContext(10, RoundingMode.HALF_EVEN));
						return PeanutsUtil.formatPercent(fraction);
					}
				}
				if (columnIndex == INVENTORY_POS_PRICE) {
					return PeanutsUtil.formatCurrency(entry.getPrice(date).getValue(), currency);
				}
				if (columnIndex == INVENTORY_POS_MARKETVALUE) {
					return PeanutsUtil.formatCurrency(entry.getMarketValue(date), currency);
				}
				if (columnIndex == INVENTORY_POS_GAIN) {
					return PeanutsUtil.formatCurrency(entry.getMarketValue(date).subtract(entry.getInvestedAmount()), currency);
				}
				if (columnIndex == INVENTORY_POS_GAIN_PERCENT) {
					if (entry.getInvestedAmount().compareTo(BigDecimal.ZERO) != 0) {
						BigDecimal gain = entry.getMarketValue(date).subtract(entry.getInvestedAmount());
						BigDecimal gainPercent = gain.divide(entry.getInvestedAmount(), new MathContext(10, RoundingMode.HALF_EVEN));
						return PeanutsUtil.formatPercent(gainPercent);
					}
				}
				if (columnIndex == INVENTORY_POS_DAY_CHANGE) {
					return PeanutsUtil.formatCurrency(entry.getChange(date.addDays(-1), date), currency);
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
					return PeanutsUtil.formatPercent(entry.getXIRR(date));
				}
			} else if (element instanceof AnalyzedInvestmentTransaction) {
				AnalyzedInvestmentTransaction t = (AnalyzedInvestmentTransaction) element;
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
			} else if (element instanceof InvestmentTransaction) {
				InvestmentTransaction t = (InvestmentTransaction) element;
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
			if (element instanceof InventoryEntry) {
				InventoryEntry entry = (InventoryEntry) element;
				if (columnIndex == INVENTORY_POS_GAIN || columnIndex == INVENTORY_POS_GAIN_PERCENT) {
					if (entry.getMarketValue(date).subtract(entry.getInvestedAmount()).signum() == -1) {
						return red;
					}
				}
				if (columnIndex == INVENTORY_POS_DAY_CHANGE) {
					if (entry.getChange(date.addDays(-1), date).signum() == -1) {
						return red;
					}
				}
				if (columnIndex == INVENTORY_POS_RATE) {
					if (entry.getXIRR(date).signum() == -1) {
						return red;
					}
				}
			} else if (element instanceof AnalyzedInvestmentTransaction) {
				AnalyzedInvestmentTransaction t = (AnalyzedInvestmentTransaction) element;
				if (columnIndex == TRANSACTION_POS_GAIN && t.getType() == InvestmentTransaction.Type.SELL) {
					if (t.getGain() != null && t.getGain().signum() == -1) {
						return red;
					}
				}
			} else if (element instanceof InvestmentTransaction) {
				InvestmentTransaction t = (InvestmentTransaction) element;
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
		restoreState();

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
		l.setText("Gainings:");
		l.setFont(boldFont);
		gainingLabel = new Label(banner, SWT.NONE);
		gainingLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		l = new Label(banner, SWT.NONE);
		l.setFont(boldFont);
		l.setText("Market value:");
		marketValueLabel = new Label(banner, SWT.NONE);
		marketValueLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		l = new Label(banner, SWT.NONE);
		l.setFont(boldFont);
		l.setText("Change:");
		changeLabel = new Label(banner, SWT.NONE);
		changeLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));


		ITransactionProvider account = getTransactions();
		if (account instanceof IConfigurable) {
			showAllSecurities = Boolean.parseBoolean(((IConfigurable)account).getConfigurationValue(SHOW_ALL_SECURITIES));
		} else {
			showAllSecurities = false;
		}

		treeViewer = new TreeViewer(top);
		Tree tree = treeViewer.getTree();
		ColumnViewerToolTipSupport.enableFor(treeViewer);
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		treeViewer.setContentProvider(new InventoryContentProvider());
		Color red = Activator.getDefault().getColorProvider().get(Activator.RED);
		treeViewer.setLabelProvider(new InventoryLabelProvider(red, account.getCurrency()));
		treeViewer.addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (showAllSecurities) {
					return true;
				}
				if (element instanceof InventoryEntry) {
					InventoryEntry entry = (InventoryEntry) element;
					return entry.getQuantity().intValue() != 0;
				}
				return true;
			}
		});

		treeViewer.setComparator(securityNameComparator);

		ControlListener saveSizeOnResize = new ControlListener() {
			@Override
			public void controlResized(ControlEvent e) {
				saveState();
			}
			@Override
			public void controlMoved(ControlEvent e) {
			}
		};

		TreeColumn col = new TreeColumn(tree, SWT.LEFT);
		col.setText("Name");
		col.setResizable(true);
		col.setWidth((colWidth[0] > 0) ? colWidth[0] : 200);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TreeColumn)e.widget, securityNameComparator);
			}
		});
		col.addControlListener(saveSizeOnResize);
		TreeViewerColumn treeViewerColumn = new TreeViewerColumn(treeViewer, col);
		treeViewerColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				Object element = cell.getElement();
				if (element instanceof InventoryEntry) {
					InventoryEntry entry = (InventoryEntry) element;
					cell.setText(entry.getSecurity().getName());
					String icon = entry.getSecurity().getConfigurationValue("icon");
					if (StringUtils.isBlank(icon)) {
						icon = "empty";
					}
					cell.setImage(Activator.getDefault().getImage("icons/"+icon+".png"));
				} else if (element instanceof InvestmentTransaction) {
					InvestmentTransaction t = (InvestmentTransaction) element;
					cell.setText(" - "+PeanutsUtil.formatDate(t.getDay()) + " ("+t.getType().toString()+")");
				}
			}
			@Override
			public String getToolTipText(Object element) {
				if (element instanceof InventoryEntry) {
					InventoryEntry entry = (InventoryEntry) element;
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
		col.setWidth((colWidth[1] > 0) ? colWidth[1] : 100);
		col.addControlListener(saveSizeOnResize);

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Quantity");
		col.setResizable(true);
		col.setWidth((colWidth[2] > 0) ? colWidth[2] : 100);
		col.addControlListener(saveSizeOnResize);

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Price");
		col.setResizable(true);
		col.setWidth((colWidth[3] > 0) ? colWidth[3] : 100);
		col.addControlListener(saveSizeOnResize);

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Market value");
		col.setResizable(true);
		col.setWidth((colWidth[4] > 0) ? colWidth[4] : 100);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TreeColumn)e.widget, marketValueComparator);
			}
		});
		col.addControlListener(saveSizeOnResize);

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Avg Price");
		col.setResizable(true);
		col.setWidth((colWidth[5] > 0) ? colWidth[5] : 100);

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Investment Sum");
		col.setResizable(true);
		col.setWidth((colWidth[6] > 0) ? colWidth[6] : 100);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TreeColumn)e.widget, investmentSumComparator);
			}
		});
		col.addControlListener(saveSizeOnResize);

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Gain/Lost");
		col.setResizable(true);
		col.setWidth((colWidth[7] > 0) ? colWidth[7] : 100);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TreeColumn)e.widget, gainingComparator);
			}
		});
		col.addControlListener(saveSizeOnResize);

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Gain/Lost (%)");
		col.setResizable(true);
		col.setWidth((colWidth[8] > 0) ? colWidth[8] : 100);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TreeColumn)e.widget, gainingPercentComparator);
			}
		});
		col.addControlListener(saveSizeOnResize);

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Rate p.a.");
		col.setResizable(true);
		col.setWidth((colWidth[9] > 0) ? colWidth[9] : 100);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TreeColumn)e.widget, ratePerYearComparator);
			}
		});
		col.addControlListener(saveSizeOnResize);

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Change");
		col.setResizable(true);
		col.setWidth((colWidth[10] > 0) ? colWidth[10] : 100);
		col.addControlListener(saveSizeOnResize);

		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection sel = (IStructuredSelection)event.getSelection();
				if (sel.getFirstElement() instanceof InventoryEntry) {
					Security security = ((InventoryEntry) sel.getFirstElement()).getSecurity();
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

		inventory = new Inventory(account, PriceProviderFactory.getInstance(), date, new AnalyzerFactory());
		inventory.setDate(date);
		inventory.addPropertyChangeListener(inventoryChangeListener);
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
						if (transactionProvider instanceof IConfigurable) {
							((IConfigurable)transactionProvider).putConfigurationValue(SHOW_ALL_SECURITIES, Boolean.valueOf(showAllSecurities).toString());
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
		gainingLabel.setText(PeanutsUtil.formatCurrency(inventory.getGainings(), account.getCurrency()));
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

	public void restoreState() {
		IConfigurable config = (IConfigurable) getEditorInput().getAdapter(IConfigurable.class);
		if (config != null) {
			for (int i = 0; i < colWidth.length; i++ ) {
				String width = config.getConfigurationValue(getClass().getSimpleName()+".col" + i);
				if (width != null) {
					colWidth[i] = Integer.valueOf(width).intValue();
				}
			}
		}
	}

	public void saveState() {
		IConfigurable config = (IConfigurable) getEditorInput().getAdapter(IConfigurable.class);
		if (config != null) {
			TreeColumn[] columns = treeViewer.getTree().getColumns();
			for (int i = 0; i < columns.length; i++ ) {
				TreeColumn tableColumn = columns[i];
				config.putConfigurationValue(getClass().getSimpleName()+".col" + i, String.valueOf(tableColumn.getWidth()));
			}
		}
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
