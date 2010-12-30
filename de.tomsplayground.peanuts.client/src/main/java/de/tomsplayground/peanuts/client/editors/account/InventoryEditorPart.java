package de.tomsplayground.peanuts.client.editors.account;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableEditor;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import de.tomsplayground.peanuts.client.util.UniqueAsyncExecution;
import de.tomsplayground.peanuts.client.widgets.DateComposite;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.base.InventoryEntry;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction.Type;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.domain.reporting.investment.AnalyzedInvestmentTransaction;
import de.tomsplayground.peanuts.domain.reporting.investment.AnalyzerFactory;
import de.tomsplayground.peanuts.util.PeanutsUtil;
import de.tomsplayground.util.Day;

public class InventoryEditorPart extends EditorPart implements IPersistableEditor {

	private static final String SHOW_ALL_SECURITIES = "inventoryShowAllSecurities";
	private int colWidth[] = new int[9];
	private TreeViewer treeViewer;
	private Label gainingLabel;
	private Label marketValueLabel;
	private Day date = new Day();

	private boolean showAllSecurities;
	private Inventory inventory;
	
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
				List<InvestmentTransaction> transations = entry.getTransations();
				Collections.reverse(transations);
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
			Collections.sort(entries, new Comparator<InventoryEntry>() {
				@Override
				public int compare(InventoryEntry o1, InventoryEntry o2) {
					return o1.getSecurity().getName().compareToIgnoreCase(
						o2.getSecurity().getName());
				}
			});
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
		
		private static final int INVENTRY_POS_NAME = 0;
		private static final int TRANSACTION_POS_DATE = 0;
		private static final int TRANSACTION_POS_QUANTITY = 1;
		private static final int INVENTRY_POS_QUANTITY = 2;
		private static final int TRANSACTION_POS_QUANTITY_SUM = 2;
		private static final int INVENTRY_POS_PRICE = 3;
		private static final int TRANSACTION_POS_PRICE = 3;
		private static final int INVENTRY_POS_MARKETVALUE = 4;
		private static final int TRANSACTION_POS_AMOUNT = 4;
		private static final int INVENTRY_POS_AVG_PRICE = 5;
		private static final int TRANSACTION_POS_AVG_PRICE = 5;
		private static final int INVENTRY_POS_INVESTED_SUM = 6;
		private static final int TRANSACTION_POS_INVESTED_SUM = 6;
		private static final int TRANSACTION_POS_GAIN = 7;
		private static final int INVENTRY_POS_GAIN = 7;
		private static final int INVENTORY_POS_GAIN_PERCENT = 8;
		
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
				if (columnIndex == INVENTRY_POS_NAME) {
					return entry.getSecurity().getName();
				}
				if (columnIndex == INVENTRY_POS_PRICE) {
					return PeanutsUtil.formatCurrency(entry.getPrice().getValue(), currency);
				}
				if (columnIndex == INVENTRY_POS_MARKETVALUE) {
					return PeanutsUtil.formatCurrency(entry.getMarketValue(), currency);
				}
				if (columnIndex == INVENTRY_POS_GAIN) {
					return PeanutsUtil.formatCurrency(entry.getMarketValue().subtract(entry.getInvestedAmount()), currency);
				}
				if (columnIndex == INVENTORY_POS_GAIN_PERCENT) {
					if (entry.getInvestedAmount().compareTo(BigDecimal.ZERO) != 0) {
						BigDecimal gain = entry.getMarketValue().subtract(entry.getInvestedAmount());
						BigDecimal gainPercent = gain.divide(entry.getInvestedAmount(), new MathContext(10, RoundingMode.HALF_EVEN));
						return PeanutsUtil.formatPercent(gainPercent);
					}
				}
				if (columnIndex == INVENTRY_POS_QUANTITY) {
					return PeanutsUtil.formatQuantity(entry.getQuantity());
				}
				if (columnIndex == INVENTRY_POS_AVG_PRICE) {
					return PeanutsUtil.formatCurrency(entry.getAvgPrice(), currency);
				}
				if (columnIndex == INVENTRY_POS_INVESTED_SUM) {
					return PeanutsUtil.formatCurrency(entry.getInvestedAmount(), currency);
				}
			} else if (element instanceof AnalyzedInvestmentTransaction) {
				AnalyzedInvestmentTransaction t = (AnalyzedInvestmentTransaction) element;
				if (columnIndex == TRANSACTION_POS_DATE) {
					return PeanutsUtil.formatDate(t.getDay());
				}
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
					return PeanutsUtil.formatCurrency(t.getAvgPrice().multiply(t.getQuantitySum()), currency);
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
				if (columnIndex == INVENTRY_POS_GAIN || columnIndex == INVENTORY_POS_GAIN_PERCENT) {
					if (entry.getMarketValue().subtract(entry.getInvestedAmount()).signum() == -1)
						return red;
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
		if ( !(input instanceof AccountEditorInput)) {
			throw new PartInitException("Invalid Input: Must be AccountEditorInput");
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
		l.setText("Gainings:");
		l.setFont(boldFont);
		gainingLabel = new Label(banner, SWT.NONE);
		gainingLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		l = new Label(banner, SWT.NONE);
		l.setFont(boldFont);
		l.setText("Market value:");
		marketValueLabel =  new Label(banner, SWT.NONE);
		marketValueLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		Account account = ((AccountEditorInput) getEditorInput()).getAccount();
		showAllSecurities = false;
		final Map<String, String> displayConfiguration = account.getDisplayConfiguration();
		if (displayConfiguration.containsKey(SHOW_ALL_SECURITIES))
			showAllSecurities = Boolean.parseBoolean(displayConfiguration.get(SHOW_ALL_SECURITIES));

		treeViewer = new TreeViewer(top);
		Tree tree = treeViewer.getTree();
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		treeViewer.setContentProvider(new InventoryContentProvider());
		Color red = getSite().getShell().getDisplay().getSystemColor(SWT.COLOR_RED);
		treeViewer.setLabelProvider(new InventoryLabelProvider(red, account.getCurrency()));
		treeViewer.addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (showAllSecurities)
					return true;
				if (element instanceof InventoryEntry) {
					InventoryEntry entry = (InventoryEntry) element;
					return entry.getQuantity().intValue() != 0;
				}
				return true;
			}
		});

		TreeColumn col = new TreeColumn(tree, SWT.LEFT);
		col.setText("Name");
		col.setResizable(true);
		col.setWidth((colWidth[0] > 0) ? colWidth[0] : 200);

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Quantity change");
		col.setResizable(true);
		col.setWidth((colWidth[1] > 0) ? colWidth[1] : 100);

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Quantity");
		col.setResizable(true);
		col.setWidth((colWidth[2] > 0) ? colWidth[2] : 100);

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Price");
		col.setResizable(true);
		col.setWidth((colWidth[3] > 0) ? colWidth[3] : 100);

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Market value");
		col.setResizable(true);
		col.setWidth((colWidth[4] > 0) ? colWidth[4] : 100);
		
		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Avg Price");
		col.setResizable(true);
		col.setWidth((colWidth[5] > 0) ? colWidth[5] : 100);

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Investment Sum");
		col.setResizable(true);
		col.setWidth((colWidth[6] > 0) ? colWidth[6] : 100);

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Gain/Lost");
		col.setResizable(true);
		col.setWidth((colWidth[7] > 0) ? colWidth[7] : 100);

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Gain/Lost (%)");
		col.setResizable(true);
		col.setWidth((colWidth[8] > 0) ? colWidth[8] : 100);

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
						displayConfiguration.put(SHOW_ALL_SECURITIES, Boolean.valueOf(showAllSecurities).toString());
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
		Account account = ((AccountEditorInput) getEditorInput()).getAccount();
		treeViewer.refresh(true);
		gainingLabel.setText(PeanutsUtil.formatCurrency(inventory.getGainings(), account.getCurrency()));
		marketValueLabel.setText(PeanutsUtil.formatCurrency(inventory.getMarketValue(), account.getCurrency()));
		marketValueLabel.getParent().layout();
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
		// nothing to do
	}

	@Override
	public void restoreState(IMemento memento) {
		for (int i = 0; i < colWidth.length; i++ ) {
			Integer width = memento.getInteger("col" + i);
			if (width != null) {
				colWidth[i] = width.intValue();
			}
		}
	}

	@Override
	public void saveState(IMemento memento) {
		TreeColumn[] columns = treeViewer.getTree().getColumns();
		for (int i = 0; i < columns.length; i++ ) {
			TreeColumn tableColumn = columns[i];
			memento.putInteger("col" + i, tableColumn.getWidth());
		}
	}

}
