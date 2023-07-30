package de.tomsplayground.peanuts.client.editors.account;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tomsplayground.peanuts.app.ib.IbConnection;
import de.tomsplayground.peanuts.app.ib.IbConnection.FullExec;
import de.tomsplayground.peanuts.app.ib.IbFlexQuery;
import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.editors.ITransactionProviderInput;
import de.tomsplayground.peanuts.config.IConfigurable;
import de.tomsplayground.peanuts.domain.option.IbOptions;
import de.tomsplayground.peanuts.domain.option.Option;
import de.tomsplayground.peanuts.domain.option.OptionsLog;
import de.tomsplayground.peanuts.domain.option.OptionsLog.LogEntry;
import de.tomsplayground.peanuts.domain.option.OptionsLog.TradesPerOption;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class OptionsLogEditorPart extends EditorPart {

	private final static Logger log = LoggerFactory.getLogger(OptionsLogEditorPart.class);

	private final int colWidth[] = new int[14];
	private TreeViewer treeViewer;
	private OptionsLog optionsFromXML;
	
	private static class OptionsLogContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			OptionsLog optionsLog = (OptionsLog) inputElement;
			Collection<TradesPerOption> trades = optionsLog.getTradePerOption().values();
			return trades.toArray();
		}

		@Override
		public boolean hasChildren(Object element) {
			return element instanceof TradesPerOption;
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof TradesPerOption entry) {
				List<LogEntry> entries = entry.getTrades();
				return entries.toArray();
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

	abstract private class OptionsLogComparator extends ViewerComparator {

		abstract int compareInventoryEntry(Viewer viewer, TradesPerOption e1, TradesPerOption e2);

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			int sort = ((TreeViewer)viewer).getTree().getSortDirection();
			if (e1 instanceof TradesPerOption  invE1 && e2 instanceof TradesPerOption invE2) {
				int compare = compareInventoryEntry(viewer, invE1, invE2);
				return (sort == SWT.DOWN) ? compare : -compare;
			}
			if (e1 instanceof LogEntry i1 && e2 instanceof LogEntry i2) {
				return i1.getDate().compareTo(i2.getDate());
			}
			return 0;
		}
	}

	private final OptionsLogComparator optionNameComparator = new OptionsLogComparator() {
		@Override
		public int compareInventoryEntry(Viewer viewer, TradesPerOption i1, TradesPerOption i2) {
			return i2.getOption().getDescription().compareToIgnoreCase(i1.getOption().getDescription());
		}
	};

	private final OptionsLogComparator optionQuantityComparator = new OptionsLogComparator() {
		@Override
		public int compareInventoryEntry(Viewer viewer, TradesPerOption i1, TradesPerOption i2) {
			return Integer.compare(i1.getQuantity(), i2.getQuantity());
		}
	};

	private final OptionsLogComparator optionExpirationComparator = new OptionsLogComparator() {
		@Override
		public int compareInventoryEntry(Viewer viewer, TradesPerOption i1, TradesPerOption i2) {
			return i1.getOption().getExpiration().compareTo(i2.getOption().getExpiration());
		}
	};
	
	private final OptionsLogComparator optionDistanceComparator = new OptionsLogComparator() {
		@Override
		public int compareInventoryEntry(Viewer viewer, TradesPerOption i1, TradesPerOption i2) {
			Option o1 = i1.getOption();
			Option o2 = i2.getOption();
			if (lastPrices.containsKey(o1) && lastPrices.containsKey(o2)) {
				BigDecimal distance1 = lastPrices.get(o1).getValue().subtract(o1.getStrike());
				distance1 = distance1.divide(o1.getStrike(), PeanutsUtil.MC);
				BigDecimal distance2 = lastPrices.get(o2).getValue().subtract(o2.getStrike());
				distance2 = distance2.divide(o2.getStrike(), PeanutsUtil.MC);
				return distance1.compareTo(distance2);
			}
			if (lastPrices.containsKey(o1)) {
				return 1;
			}
			if (lastPrices.containsKey(o2)) {
				return -1;
			}
			return 0;
		}
	};

	private class OptionsLogLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider {

		private static Color red = Activator.getDefault().getColorProvider().get(Activator.RED);
		
		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof TradesPerOption tradesPerOption) {
				Option option = tradesPerOption.getOption();
				if (columnIndex == 0) {
					return option.getDescription();
				} else if (columnIndex == 1) {
					return PeanutsUtil.formatQuantity(new BigDecimal(tradesPerOption.getQuantity()));
				} else if (columnIndex == 2) {
					return Integer.toString(Day.today().delta(option.getExpiration()));
				} else if (columnIndex == 3) {
					return PeanutsUtil.formatCurrency(tradesPerOption.getCostBaseCurrency(), ((AccountEditorInput)getEditorInput()).account.getCurrency());
				} else if (columnIndex == 4) {
					if (lastPrices.containsKey(option)) {
						return PeanutsUtil.formatCurrency(lastPrices.get(option).getValue(), null);
					}
				} else if (columnIndex == 5) {
					if (lastPrices.containsKey(option)) {
						BigDecimal distance = lastPrices.get(option).getValue().subtract(option.getStrike());
						distance = distance.divide(option.getStrike(), PeanutsUtil.MC);
						return PeanutsUtil.formatPercent(distance);
					}					
				}
			} else  if (element instanceof LogEntry logEntry) {
				if (columnIndex == 0) {
					return logEntry.getDate().toString();
				} else if (columnIndex == 1) {
					return PeanutsUtil.formatQuantity(new BigDecimal(logEntry.getQuantity()));
				}
			}
			return null;
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			if (element instanceof TradesPerOption tradesPerOption) {
				if (columnIndex == 1) {
					if (tradesPerOption.getQuantity() < 0) {
						return red;
					}
				} else if (columnIndex == 2) {
					if (Day.today().delta(tradesPerOption.getOption().getExpiration()) <= 10) {
						return red;
					}
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

	private final Map<Option, Price> lastPrices = new HashMap<>();
	
	private void loadIbData() {
		IbConnection ibConnection = new IbConnection();
		ibConnection.start();
		while (! ibConnection.isConnected()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				return;
			}
		}
		List<FullExec> tradeLog = ibConnection.getTradeLog();
		new IbOptions().optionsFromIb(tradeLog, optionsFromXML);
		optionsFromXML.doit();
		
		for (Option entry : optionsFromXML.getTradePerOption().keySet()) {
			int underlyingConId = entry.getUnderlyingConId();
			try {
				Price lastPrice = ibConnection.getLastPrice(entry.getUnderlying(), entry.getUnderlyingExchange(), underlyingConId);
				if (lastPrice != null) {
					lastPrices.put(entry, lastPrice);
				} else {
					log.error("No price found for {} @{} ({})", entry.getUnderlying(), entry.getUnderlyingExchange(), underlyingConId);
				}
			} catch (InterruptedException e) {
				return;
			}
		}
		ibConnection.stop();
		
		getSite().getShell().getDisplay().asyncExec(() -> treeViewer.refresh(true));
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
		final Composite banner = new Composite(top, SWT.NONE);
		banner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		layout = new GridLayout();
		layout.marginHeight = 5;
		layout.marginWidth = 10;
		layout.numColumns = 3;
		banner.setLayout(layout);

		Label l = new Label(banner, SWT.WRAP);
		l.setText("Type:");
		
		final Combo typeCombo = new Combo(banner, SWT.DROP_DOWN | SWT.READ_ONLY);
		typeCombo.add("All");
		typeCombo.add("Long");
		typeCombo.add("Short");
		typeCombo.select(0);
		typeCombo.addModifyListener(e -> {
			CompletableFuture.runAsync(() -> loadIbData());
			String typeString = ((Combo)e.widget).getText();
			if (typeString.equals("All")) {
				treeViewer.resetFilters();
			} else {
				treeViewer.setFilters(new ViewerFilter() {
					@Override
					public boolean select(Viewer viewer, Object parentElement, Object element) {
						if (element instanceof TradesPerOption tradesPerOption) {
							return (tradesPerOption.getQuantity() > 0) == typeString.equals("Long");
						}
						return true;
					}
				});
			}
		});

		treeViewer = new TreeViewer(top);
		Tree tree = treeViewer.getTree();
		ColumnViewerToolTipSupport.enableFor(treeViewer);
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tree.setFont(Activator.getDefault().getNormalFont());
		treeViewer.setContentProvider(new OptionsLogContentProvider());

		ControlListener saveSizeOnResize = new ControlListener() {
			@Override
			public void controlResized(ControlEvent e) {
				saveState();
			}
			@Override
			public void controlMoved(ControlEvent e) {
			}
		};

		int colNumber = 0;
		TreeColumn col = new TreeColumn(tree, SWT.LEFT);
		
		col.setText("Name");
		col.setResizable(true);
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 200);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TreeColumn)e.widget, optionNameComparator);
			}
		});
		col.addControlListener(saveSizeOnResize);
		treeViewer.getTree().setSortColumn(col);
		treeViewer.getTree().setSortDirection(SWT.UP);
		treeViewer.setLabelProvider(new OptionsLogLabelProvider());

		colNumber++;
		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Quantity");
		col.setResizable(true);
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TreeColumn)e.widget, optionQuantityComparator);
			}
		});
		col.addControlListener(saveSizeOnResize);
		
		colNumber++;
		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Resttage");
		col.setResizable(true);
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TreeColumn)e.widget, optionExpirationComparator);
			}
		});
		col.addControlListener(saveSizeOnResize);

		colNumber++;
		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Kosten");
		col.setResizable(true);
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.addControlListener(saveSizeOnResize);

		colNumber++;
		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Preis");
		col.setResizable(true);
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.addControlListener(saveSizeOnResize);

		colNumber++;
		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Abstand %");
		col.setResizable(true);
		col.setWidth((colWidth[colNumber] > 0) ? colWidth[colNumber] : 100);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TreeColumn)e.widget, optionDistanceComparator);
			}
		});
		col.addControlListener(saveSizeOnResize);

		String filename = "/Users/quelle/Documents/Geld/InteractiveBrokers/FlexQuery_2023.xml";
		optionsFromXML = new IbFlexQuery().readOptionsFromXML(filename);
		treeViewer.setInput(optionsFromXML);
		
		CompletableFuture.runAsync(() -> loadIbData());
	}

	public void restoreState() {
		IConfigurable config = getEditorInput().getAdapter(IConfigurable.class);
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
		IConfigurable config = getEditorInput().getAdapter(IConfigurable.class);
		if (config != null) {
			TreeColumn[] columns = treeViewer.getTree().getColumns();
			for (int i = 0; i < columns.length; i++ ) {
				TreeColumn tableColumn = columns[i];
				config.putConfigurationValue(getClass().getSimpleName()+".col" + i, String.valueOf(tableColumn.getWidth()));
			}
		}
	}

	protected void setSorting(TreeColumn column, OptionsLogComparator newComparator) {
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

	@Override
	public void setFocus() {
	}

	@Override
	public boolean isDirty() {
		return false;
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

}
