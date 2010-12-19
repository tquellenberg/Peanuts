package de.tomsplayground.peanuts.client.views;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
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

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.dnd.PeanutsTransfer;
import de.tomsplayground.peanuts.client.dnd.SecurityTransferData;
import de.tomsplayground.peanuts.client.util.UniqueAsyncExecution;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.domain.statistics.Signal;
import de.tomsplayground.peanuts.domain.statistics.Signal.Type;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class SecurityWatchlistView extends ViewPart {

	public static final String ID = "de.tomsplayground.peanuts.client.securityWatchListView";
	
	private TableViewer securityListViewer;
	private List<WatchEntry> watchList;
	private final int colWidth[] = new int[11];

	private final PropertyChangeListener priceProviderChangeListener = new UniqueAsyncExecution() {
		@Override
		public void doit(PropertyChangeEvent evt, Display display) {
			securityListViewer.refresh();
		}

		@Override
		public Display getDisplay() {
			return securityListViewer.getTable().getDisplay();
		}
	};

	private class SecurityListLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider {

		private Color red;
		private Color green;
		
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
					if (price == null)
						return "";
					return PeanutsUtil.formatDate(price.getDay());
			case 2:
					Price price2 = watchEntry.getPrice();
					if (price2 == null)
						return "";
					return PeanutsUtil.formatCurrency(price2.getClose(), null);
			case 3:
				Signal signal = watchEntry.getSignal();
				if (signal != null)
					return signal.getType().toString() + " " + PeanutsUtil.formatDate(signal.getPrice().getDay());
				return "";
			case 4:
				return PeanutsUtil.formatCurrency(watchEntry.getDayChangeAbsolut(), null);
			case 5:
				return PeanutsUtil.formatPercent(watchEntry.getDayChange());
			case 6:
				return PeanutsUtil.formatPercent(watchEntry.getPerformance(7, 0, 0));
			case 7:
				return PeanutsUtil.formatPercent(watchEntry.getPerformance(0, 1, 0));
			case 8:
				return PeanutsUtil.formatPercent(watchEntry.getPerformance(0, 6, 0));
			case 9:
				return PeanutsUtil.formatPercent(watchEntry.getPerformance(0, 0, 1));
			case 10:
				return PeanutsUtil.formatPercent(watchEntry.getPerformance(0, 0, 3));
			default:
				break;
			}
			return null;
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			if (columnIndex == 3) {
				WatchEntry watchEntry = (WatchEntry) element;
				if (watchEntry.getSignal() != null) {
					if (watchEntry.getSignal().getType() == Type.BUY)
						return green;
					if (watchEntry.getSignal().getType() == Type.SELL)
						return red;
				}
			}
			return null;
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			WatchEntry watchEntry = (WatchEntry) element;
			if (columnIndex == 4 || columnIndex == 5) {
				return (watchEntry.getDayChangeAbsolut().signum() == -1) ? red : green;
			} else if (columnIndex == 6) {
				return (watchEntry.getPerformance(7, 0, 0).signum() == -1) ? red : green;
			} else if (columnIndex == 7) {
				return (watchEntry.getPerformance(0, 1, 0).signum() == -1) ? red : green;
			} else if (columnIndex == 8) {
				return (watchEntry.getPerformance(0, 6, 0).signum() == -1) ? red : green;
			} else if (columnIndex == 9) {
				return (watchEntry.getPerformance(0, 0, 1).signum() == -1) ? red : green;
			} else if (columnIndex == 10) {
				return (watchEntry.getPerformance(0, 0, 3).signum() == -1) ? red : green;
			}
			return null;
		}
	}
	
	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
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
		
		securityListViewer.setContentProvider(new ArrayContentProvider());
		Color red = parent.getShell().getDisplay().getSystemColor(SWT.COLOR_RED);
		Color green = parent.getShell().getDisplay().getSystemColor(SWT.COLOR_GREEN);
		securityListViewer.setLabelProvider(new SecurityListLabelProvider(red, green));
		
		securityListViewer.getTable().addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == SWT.DEL) {
					int selectionIndex = ((Table)e.getSource()).getSelectionIndex();
					if (selectionIndex >= 0) {
						securityListViewer.getTable().remove(selectionIndex);
						WatchEntry entry = watchList.remove(selectionIndex);
						IPriceProvider priceProvider = entry.getPriceProvider();
						if (priceProvider instanceof ObservableModelObject) {
							ObservableModelObject ob = (ObservableModelObject) priceProvider;
							ob.removePropertyChangeListener(priceProviderChangeListener);
						}
						entry.getSecurity().getDisplayConfiguration().remove(SecurityWatchlistView.class.getName());
					}
				}
			}
		});
		
		// Drop-Target
		Transfer[] types = new Transfer[] { PeanutsTransfer.INSTANCE };
		int operations = DND.DROP_DEFAULT | DND.DROP_LINK;
		DropTarget target = new DropTarget(securityListViewer.getTable(), operations);
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
					security.getDisplayConfiguration().put(SecurityWatchlistView.class.getName(), "1");					
					watchList.add(createWatchEntry(security));
					securityListViewer.refresh();
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
		
		createWatchlist();
		securityListViewer.setInput(watchList);
	}

	@Override
	public void dispose() {
		for (WatchEntry entry : watchList) {
			IPriceProvider priceProvider = entry.getPriceProvider();
			if (priceProvider instanceof ObservableModelObject) {
				ObservableModelObject ob = (ObservableModelObject) priceProvider;
				ob.removePropertyChangeListener(priceProviderChangeListener);
			}
		}
	}

	private void createWatchlist() {
		watchList = new ArrayList<WatchEntry>();
		List<Security> allSecurities = Activator.getDefault().getAccountManager().getSecurities();
		for (Security security : allSecurities) {
			if (security.getDisplayConfiguration().get(SecurityWatchlistView.class.getName()) != null) {
				watchList.add(createWatchEntry(security));
			}
		}
	}

	private WatchEntry createWatchEntry(Security security) {
		IPriceProvider priceProvider = PriceProviderFactory.getInstance().getPriceProvider(security);
		if (priceProvider instanceof ObservableModelObject) {
			ObservableModelObject ob = (ObservableModelObject) priceProvider;
			ob.addPropertyChangeListener(priceProviderChangeListener);
		}
		WatchEntry watchEntry = new WatchEntry(security, priceProvider);
		return watchEntry;
	}

	@Override
	public void setFocus() {
		// nothing to do
	}

}
