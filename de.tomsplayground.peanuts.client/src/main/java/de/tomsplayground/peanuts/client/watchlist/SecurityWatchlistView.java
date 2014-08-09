package de.tomsplayground.peanuts.client.watchlist;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.dnd.PeanutsTransfer;
import de.tomsplayground.peanuts.client.dnd.SecurityTransferData;
import de.tomsplayground.peanuts.client.util.UniqueAsyncExecution;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.fundamental.FundamentalData;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.domain.statistics.Signal;
import de.tomsplayground.peanuts.domain.statistics.Signal.Type;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class SecurityWatchlistView extends ViewPart {

	public static final String ID = "de.tomsplayground.peanuts.client.securityWatchListView";
	
	private TableViewer securityListViewer;
	private Watchlist currentWatchList;
	private final int colWidth[] = new int[13];

	private final PropertyChangeListener watchlistChangeListener = new UniqueAsyncExecution() {
		
		@Override
		public Display getDisplay() {
			return securityListViewer.getTable().getDisplay();
		}
		
		@Override
		public void doit(PropertyChangeEvent evt, Display display) {
			if (evt.getPropertyName().equals("currentWatchlist")) {
				currentWatchList = (Watchlist) evt.getNewValue();
				setContentDescription(currentWatchList.getName());
				securityListViewer.setInput(currentWatchList);
				securityListViewer.refresh();
			} else {
				securityListViewer.refresh();
			}
		}
	};

	private class WatchlistContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			Watchlist  watchlist = (Watchlist)inputElement;
			return watchlist.getEntries().toArray();
		}
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// Nothing to do
		}
		@Override
		public void dispose() {
			// Nothing to do
		}
	}
	
	private class SecurityListLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider {

		private final Color red;
		private final Color green;
		
		public SecurityListLabelProvider(Color red, Color green) {
			this.red = red;
			this.green = green;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			WatchEntry watchEntry = (WatchEntry) element;
			switch (columnIndex) {
			case 0:
				return watchEntry.getSecurity().getName();
			case 1:
					Price price = watchEntry.getPrice();
					if (price == null) {
						return "";
					}
					return PeanutsUtil.formatDate(price.getDay());
			case 2:
					Price price2 = watchEntry.getPrice();
					if (price2 == null) {
						return "";
					}
					return PeanutsUtil.formatCurrency(price2.getClose(), null);
			case 3:
				FundamentalData data1 = watchEntry.getSecurity().getCurrentFundamentalData();
				if (data1 != null) {
					IPriceProvider priceProvider = PriceProviderFactory.getInstance().getPriceProvider(watchEntry.getSecurity());
					return PeanutsUtil.formatQuantity(data1.calculatePeRatio(priceProvider));
				}
				return "";
			case 4:
				FundamentalData data2 = watchEntry.getSecurity().getCurrentFundamentalData();
				if (data2 != null) {
					IPriceProvider priceProvider = PriceProviderFactory.getInstance().getPriceProvider(watchEntry.getSecurity());
					return PeanutsUtil.formatPercent(data2.calculateDivYield(priceProvider));
				}
				return "";
			case 5:
				Signal signal = watchEntry.getSignal();
				if (signal != null) {
					return signal.type.toString() + " " + PeanutsUtil.formatDate(signal.price.getDay());
				}
				return "";
			case 6:
				return PeanutsUtil.formatCurrency(watchEntry.getDayChangeAbsolut(), null);
			case 7:
				return PeanutsUtil.formatPercent(watchEntry.getDayChange());
			case 8:
				return PeanutsUtil.formatPercent(watchEntry.getPerformance(7, 0, 0));
			case 9:
				return PeanutsUtil.formatPercent(watchEntry.getPerformance(0, 1, 0));
			case 10:
				return PeanutsUtil.formatPercent(watchEntry.getPerformance(0, 6, 0));
			case 11:
				return PeanutsUtil.formatPercent(watchEntry.getPerformance(0, 0, 1));
			case 12:
				return PeanutsUtil.formatPercent(watchEntry.getPerformance(0, 0, 3));
			default:
				break;
			}
			return null;
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			if (columnIndex == 5) {
				WatchEntry watchEntry = (WatchEntry) element;
				if (watchEntry.getSignal() != null) {
					if (watchEntry.getSignal().type == Type.BUY) {
						return green;
					}
					if (watchEntry.getSignal().type == Type.SELL) {
						return red;
					}
				}
			}
			return null;
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			WatchEntry watchEntry = (WatchEntry) element;
			if (columnIndex == 6 || columnIndex == 7) {
				return (watchEntry.getDayChangeAbsolut().signum() == -1) ? red : green;
			} else if (columnIndex == 8) {
				return (watchEntry.getPerformance(7, 0, 0).signum() == -1) ? red : green;
			} else if (columnIndex == 9) {
				return (watchEntry.getPerformance(0, 1, 0).signum() == -1) ? red : green;
			} else if (columnIndex == 10) {
				return (watchEntry.getPerformance(0, 6, 0).signum() == -1) ? red : green;
			} else if (columnIndex == 11) {
				return (watchEntry.getPerformance(0, 0, 1).signum() == -1) ? red : green;
			} else if (columnIndex == 12) {
				return (watchEntry.getPerformance(0, 0, 3).signum() == -1) ? red : green;
			}
			return null;
		}
	}
	
	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		setContentDescription(WatchlistManager.getInstance().getCurrentWatchlist().getName());
		if (memento != null) {
			for (int i = 0; i < colWidth.length; i++ ) {
				Integer width = memento.getInteger("col" + i);
				if (width != null) {
					colWidth[i] = width.intValue();
				}
			}
		}
	}

	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		TableColumn[] columns = securityListViewer.getTable().getColumns();
		for (int i = 0; i < columns.length; i++ ) {
			TableColumn tableColumn = columns[i];
			memento.putInteger("col" + i, tableColumn.getWidth());
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		securityListViewer = new TableViewer(parent, SWT.MULTI | SWT.FULL_SELECTION);
		Table table = securityListViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		int colNum = 0;
		TableColumn col = new TableColumn(table, SWT.LEFT);
		col.setText("Name");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 300);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Date");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 100);
		col.setResizable(true);
		colNum++;
		
		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Price");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 100);
		col.setResizable(true);
		colNum++;
		
		col = new TableColumn(table, SWT.RIGHT);
		col.setText("P/E ratio");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 100);
		col.setResizable(true);
		colNum++;
		
		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Div yield");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 100);
		col.setResizable(true);
		colNum++;
		
		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Signal");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 100);
		col.setResizable(true);
		colNum++;
		
		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Change");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 100);
		col.setResizable(true);
		colNum++;
		
		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Change");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 100);
		col.setResizable(true);
		colNum++;
		
		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Perf. 1 week");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 100);
		col.setResizable(true);
		colNum++;
		
		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Perf. 1 month");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 100);
		col.setResizable(true);
		colNum++;
		
		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Perf. 6 month");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 100);
		col.setResizable(true);
		colNum++;
		
		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Perf. 1 year");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 100);
		col.setResizable(true);
		colNum++;
		
		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Perf. 3 years");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 100);
		col.setResizable(true);
		
		securityListViewer.setContentProvider(new WatchlistContentProvider());
		Color red = parent.getShell().getDisplay().getSystemColor(SWT.COLOR_RED);
		Color green = parent.getShell().getDisplay().getSystemColor(SWT.COLOR_GREEN);
		securityListViewer.setLabelProvider(new SecurityListLabelProvider(red, green));
		
		// Drop-Target
		Transfer[] types = new Transfer[] { PeanutsTransfer.INSTANCE };
		int operations = DND.DROP_DEFAULT | DND.DROP_LINK;
		DropTarget target = new DropTarget(table, operations);
		target.setTransfer(types);
		target.addDropListener(new DropTargetAdapter() {
			@Override
			public void drop(DropTargetEvent event) {
				if (event.data == null) {
					event.detail = DND.DROP_NONE;
					return;
				}
				if (event.data instanceof SecurityTransferData) {
					Security security = ((SecurityTransferData) event.data).getSecurity();
					addSecurityToCurrentWatchlist(security);
				}
			}

			@Override
			public void dragEnter(DropTargetEvent event) {
				if (event.detail == DND.DROP_DEFAULT) {
					event.detail = DND.DROP_LINK;
				}
			}

			@Override
			public void dragOperationChanged(DropTargetEvent event) {
				if (event.detail == DND.DROP_DEFAULT) {
					event.detail = DND.DROP_LINK;
				}
			}
		});
		
		initWatchlists();
		securityListViewer.setInput(currentWatchList);

		MenuManager menuManager = new MenuManager();
		table.setMenu(menuManager.createContextMenu(table));
		getSite().registerContextMenu(menuManager, securityListViewer);
		getSite().setSelectionProvider(securityListViewer);
	}
	
	@Override
	public void dispose() {
		WatchlistManager.getInstance().removePropertyChangeListener(watchlistChangeListener);
	}

	private void initWatchlists() {
		ImmutableList<Security> allSecurities = Activator.getDefault().getAccountManager().getSecurities();
		for (Security security : allSecurities) {
			for (String watchlistName : getWatchlistNamesForSecurity(security)) {
				Watchlist watchlist = WatchlistManager.getInstance().getWatchlist(watchlistName);
				if (watchlist == null) {
					watchlist = WatchlistManager.getInstance().addWatchlist(watchlistName);
				}
				watchlist.addEntry(security);
			}
		}
		currentWatchList = WatchlistManager.getInstance().getCurrentWatchlist();		
		WatchlistManager.getInstance().addPropertyChangeListener(watchlistChangeListener);
	}

	public void removeSecurityFromCurrentWatchlist(Security security) {
		// remove from Model
		WatchEntry entry = currentWatchList.removeEntry(security);
		// remove from viewer
		securityListViewer.remove(entry);
		// update DisplayConfiguration
		Set<String> list = new HashSet<String>(getWatchlistNamesForSecurity(security));
		list.remove(currentWatchList.getName());
		security.putConfigurationValue(ID, StringUtils.join(list, ','));
	}

	private void addSecurityToCurrentWatchlist(Security security) {
		// add to model
		WatchEntry entry = currentWatchList.addEntry(security);
		// update viewer
		if (entry != null) {
			securityListViewer.add(entry);
			securityListViewer.reveal(entry);
		}
		// update DisplayConfiguration
		Set<String> list = new HashSet<String>(getWatchlistNamesForSecurity(security));
		list.add(currentWatchList.getName());
		security.putConfigurationValue(ID, StringUtils.join(list, ','));
	}

	private List<String> getWatchlistNamesForSecurity(Security security) {
		String watchListsStr = security.getConfigurationValue(ID);
		String[] watchLists = StringUtils.split(watchListsStr, ',');
		if (watchLists != null) {
			return Arrays.asList(watchLists);
		}
		return Collections.emptyList();
	}
	
	@Override
	public void setFocus() {
		// nothing to do
	}

}
