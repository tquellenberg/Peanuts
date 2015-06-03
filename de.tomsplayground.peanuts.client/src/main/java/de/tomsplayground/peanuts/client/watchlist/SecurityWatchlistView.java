package de.tomsplayground.peanuts.client.watchlist;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.part.ViewPart;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.dnd.PeanutsTransfer;
import de.tomsplayground.peanuts.client.dnd.SecurityTransferData;
import de.tomsplayground.peanuts.client.editors.security.SecurityEditor;
import de.tomsplayground.peanuts.client.editors.security.SecurityEditorInput;
import de.tomsplayground.peanuts.client.util.UniqueAsyncExecution;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.domain.statistics.Signal;
import de.tomsplayground.peanuts.domain.statistics.Signal.Type;
import de.tomsplayground.peanuts.util.PeanutsUtil;
import de.tomsplayground.util.Day;

public class SecurityWatchlistView extends ViewPart {

	private static final PerformanceComparator THREE_YEAR_COMPARATOR = new PerformanceComparator(0, 0, 3);

	private static final PerformanceComparator YEAR_COMPARATOR = new PerformanceComparator(0, 0, 1);

	private static final PerformanceComparator SIX_MONTH_COMPARATOR = new PerformanceComparator(0, 6, 0);

	private static final PerformanceComparator MONTH_COMPARATOR = new PerformanceComparator(0, 1, 0);

	private static final PerformanceComparator WEEK_COMPARATOR = new PerformanceComparator(7, 0, 0);

	public static final String ID = "de.tomsplayground.peanuts.client.securityWatchListView";

	private TableViewer securityListViewer;
	private Watchlist currentWatchList;
	private final int colWidth[] = new int[19];

	private static abstract class WatchEntryViewerComparator extends ViewerComparator {
		enum SORT {
			UP, DOWN
		}

		private SORT sort = SORT.UP;

		abstract public int compare(WatchEntry w1, WatchEntry w2);

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (e1 instanceof WatchEntry && e2 instanceof WatchEntry) {
				WatchEntry w1 = (WatchEntry) e1;
				WatchEntry w2 = (WatchEntry) e2;
				int compare = compare(w1, w2);
				return (sort == SORT.DOWN) ? compare : -compare;
			}
			return 0;
		}
		public void setSortDirection(SORT sort) {
			this.sort = sort;
		}
	}

	private final WatchEntryViewerComparator nameComparator = new WatchEntryViewerComparator() {
		@Override
		public int compare(WatchEntry w1, WatchEntry w2) {
			return w2.getSecurity().getName().compareToIgnoreCase(w1.getSecurity().getName());
		}
	};
	private final WatchEntryViewerComparator peRatioComparator = new WatchEntryViewerComparator() {
		@Override
		public int compare(WatchEntry w1, WatchEntry w2) {
			BigDecimal peRatio1 = w1.getPeRatio();
			BigDecimal peRatio2 = w2.getPeRatio();
			return ObjectUtils.compare(peRatio2, peRatio1, true);
		}
	};
	private final WatchEntryViewerComparator peAvgComparator = new WatchEntryViewerComparator() {
		@Override
		public int compare(WatchEntry w1, WatchEntry w2) {
			BigDecimal peRatio1 = w1.getAvgPE();
			BigDecimal peRatio2 = w2.getAvgPE();
			return ObjectUtils.compare(peRatio2, peRatio1, true);
		}
	};
	private final WatchEntryViewerComparator peDeltaComparator = new WatchEntryViewerComparator() {
		@Override
		public int compare(WatchEntry w1, WatchEntry w2) {
			BigDecimal peRatio1 = w1.getPeDelta();
			BigDecimal peRatio2 = w2.getPeDelta();
			return ObjectUtils.compare(peRatio2, peRatio1, true);
		}
	};
	private final WatchEntryViewerComparator earningsComparator = new WatchEntryViewerComparator() {
		@Override
		public int compare(WatchEntry w1, WatchEntry w2) {
			BigDecimal peRatio1 = w1.getCurrencyAdjustedAvgEpsChange();
			BigDecimal peRatio2 = w2.getCurrencyAdjustedAvgEpsChange();
			return ObjectUtils.compare(peRatio2, peRatio1, true);
		}
	};
	private final WatchEntryViewerComparator divYieldComparator = new WatchEntryViewerComparator() {
		@Override
		public int compare(WatchEntry w1, WatchEntry w2) {
			BigDecimal divYield1 = w1.getDivYield();
			BigDecimal divYield2 = w2.getDivYield();
			return ObjectUtils.compare(divYield1, divYield2);
		}
	};
	private final WatchEntryViewerComparator yocComparator = new WatchEntryViewerComparator() {
		@Override
		public int compare(WatchEntry w1, WatchEntry w2) {
			Inventory inventory = Activator.getDefault().getAccountManager().getFullInventory();
			BigDecimal yoc1 = w1.getYOC(inventory.getEntry(w1.getSecurity()));
			BigDecimal yoc2 = w2.getYOC(inventory.getEntry(w2.getSecurity()));
			return ObjectUtils.compare(yoc1, yoc2);
		}
	};
	private final WatchEntryViewerComparator deRatioComparator = new WatchEntryViewerComparator() {
		@Override
		public int compare(WatchEntry w1, WatchEntry w2) {
			BigDecimal deRatio1 = w1.getDebtEquityRatio();
			BigDecimal deRatio2 = w2.getDebtEquityRatio();
			return ObjectUtils.compare(deRatio1, deRatio2);
		}
	};
	private static final class PerformanceComparator extends WatchEntryViewerComparator{
		private final int day;
		private final int month;
		private final int year;
		public PerformanceComparator(int day, int month, int year) {
			this.day = day;
			this.month = month;
			this.year = year;
		}
		@Override
		public int compare(WatchEntry w1, WatchEntry w2) {
			return w1.getPerformance(day, month, year).compareTo(w2.getPerformance(day, month, year));
		}
	}
	private final WatchEntryViewerComparator customPerformanceComparator = new WatchEntryViewerComparator() {
		@Override
		public int compare(WatchEntry w1, WatchEntry w2) {
			return w1.getCustomPerformance().compareTo(w2.getCustomPerformance());
		}
	};

	private final PropertyChangeListener watchlistChangeListener = new UniqueAsyncExecution() {

		@Override
		public Display getDisplay() {
			return securityListViewer.getTable().getDisplay();
		}

		@Override
		public void doit(PropertyChangeEvent evt, Display display) {
			if (! securityListViewer.getTable().isDisposed()) {
				if (evt.getPropertyName().equals("currentWatchlist")) {
					currentWatchList = (Watchlist) evt.getNewValue();
					setContentDescription(currentWatchList.getName());
					securityListViewer.setInput(currentWatchList);
					securityListViewer.refresh();
				} else {
					securityListViewer.refresh();
				}
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
		private final Color redBg;
		private final Color greenBg;

		public SecurityListLabelProvider() {
			red = Activator.getDefault().getColorProvider().get(Activator.RED);
			green = Activator.getDefault().getColorProvider().get(Activator.GREEN);
			redBg = Activator.getDefault().getColorProvider().get(Activator.RED_BG);
			greenBg = Activator.getDefault().getColorProvider().get(Activator.GREEN_BG);
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
					Security security = watchEntry.getSecurity();
					return security.getName();
				case 1:
					IPrice price = watchEntry.getPrice();
					if (price == null) {
						return "";
					}
					return PeanutsUtil.formatDate(price.getDay());
				case 2:
					IPrice price2 = watchEntry.getPrice();
					if (price2 == null) {
						return "";
					}
					return PeanutsUtil.formatCurrency(price2.getClose(), null);
				case 3:
					BigDecimal data1 = watchEntry.getPeRatio();
					if (data1 != null) {
						return PeanutsUtil.format(data1, 1);
					}
					return "";
				case 4:
					BigDecimal avgPe = watchEntry.getAvgPE();
					if (avgPe != null) {
						return PeanutsUtil.format(avgPe, 1);
					}
					return "";
				case 5:
					BigDecimal peDelta = watchEntry.getPeDelta();
					if (peDelta != null) {
						return PeanutsUtil.formatPercent(peDelta);
					}
					return "";
				case 6:
					BigDecimal v = watchEntry.getCurrencyAdjustedAvgEpsChange();
					if (v != null) {
						return PeanutsUtil.formatPercent(v);
					}
					return "";
				case 7:
					BigDecimal data2 = watchEntry.getDivYield();
					if (data2 != null) {
						return PeanutsUtil.formatPercent(data2);
					}
					return "";
				case 8:
					Inventory inventory = Activator.getDefault().getAccountManager().getFullInventory();
					BigDecimal data3 = watchEntry.getYOC(inventory.getEntry(watchEntry.getSecurity()));
					if (data3 != null) {
						return PeanutsUtil.formatPercent(data3);
					}
					return "";
				case 9:
					BigDecimal data4 = watchEntry.getDebtEquityRatio();
					if (data4 != null) {
						return PeanutsUtil.format(data4, 2);
					}
					return "";
				case 10:
					Signal signal = watchEntry.getSignal();
					if (signal != null) {
						return signal.type.toString() + " " + PeanutsUtil.formatDate(signal.price.getDay());
					}
					return "";
				case 11:
					return PeanutsUtil.formatCurrency(watchEntry.getDayChangeAbsolut(), null);
				case 12:
					return PeanutsUtil.formatPercent(watchEntry.getDayChange());
				case 13:
					return PeanutsUtil.formatPercent(watchEntry.getPerformance(7, 0, 0));
				case 14:
					return PeanutsUtil.formatPercent(watchEntry.getPerformance(0, 1, 0));
				case 15:
					return PeanutsUtil.formatPercent(watchEntry.getPerformance(0, 6, 0));
				case 16:
					return PeanutsUtil.formatPercent(watchEntry.getPerformance(0, 0, 1));
				case 17:
					return PeanutsUtil.formatPercent(watchEntry.getPerformance(0, 0, 3));
				case 18:
					return PeanutsUtil.formatPercent(watchEntry.getCustomPerformance());
				default:
					break;
			}
			return null;
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			if (columnIndex == 10) {
				WatchEntry watchEntry = (WatchEntry) element;
				if (watchEntry.getSignal() != null) {
					if (watchEntry.getSignal().type == Type.BUY) {
						return greenBg;
					}
					if (watchEntry.getSignal().type == Type.SELL) {
						return redBg;
					}
				}
			}
			return null;
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			WatchEntry watchEntry = (WatchEntry) element;
			if (columnIndex == 5) {
				BigDecimal peDelta = watchEntry.getPeDelta();
				if (peDelta != null) {
					if (peDelta.compareTo(new BigDecimal(0.05)) > 0) {
						return red;
					}
					if (peDelta.compareTo(new BigDecimal(-0.05)) < 0) {
						return green;
					}
				}
			} else if (columnIndex == 6) {
				BigDecimal v = watchEntry.getCurrencyAdjustedAvgEpsChange();
				if (v != null) {
					if (v.compareTo(new BigDecimal(0.03)) > 0) {
						return green;
					}
					if (v.compareTo(new BigDecimal(-0.03)) < 0) {
						return red;
					}
				}
			} else if (columnIndex == 9) {
				BigDecimal data4 = watchEntry.getDebtEquityRatio();
				if (data4 != null) {
					if (data4.intValue() > 100) {
						return red;
					}
					if (data4.intValue() < 40) {
						return green;
					}
				}
			} else if (columnIndex == 11 || columnIndex == 12) {
				return (watchEntry.getDayChangeAbsolut().signum() == -1) ? red : green;
			} else if (columnIndex == 13) {
				return (watchEntry.getPerformance(7, 0, 0).signum() == -1) ? red : green;
			} else if (columnIndex == 14) {
				return (watchEntry.getPerformance(0, 1, 0).signum() == -1) ? red : green;
			} else if (columnIndex == 15) {
				return (watchEntry.getPerformance(0, 6, 0).signum() == -1) ? red : green;
			} else if (columnIndex == 16) {
				return (watchEntry.getPerformance(0, 0, 1).signum() == -1) ? red : green;
			} else if (columnIndex == 17) {
				return (watchEntry.getPerformance(0, 0, 3).signum() == -1) ? red : green;
			} else if (columnIndex == 18) {
				return (watchEntry.getCustomPerformance().signum() == -1) ? red : green;
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
			WatchlistManager watchlistManager = WatchlistManager.getInstance();
			String fromStr = memento.getString("customPerformance.from");
			if (StringUtils.isNoneBlank(fromStr)) {
				watchlistManager.setPerformanceFrom(Day.fromString(fromStr));
			}
			String toStr = memento.getString("customPerformance.to");
			if (StringUtils.isNoneBlank(toStr)) {
				watchlistManager.setPerformanceTo(Day.fromString(toStr));
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
		WatchlistManager watchlistManager = WatchlistManager.getInstance();
		if (watchlistManager.isCustomPerformanceRangeSet()) {
			memento.putString("customPerformance.from", watchlistManager.getPerformanceFrom().toString());
			memento.putString("customPerformance.to", watchlistManager.getPerformanceTo().toString());
		} else {
			memento.putString("customPerformance.from", "");
			memento.putString("customPerformance.to", "");
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		securityListViewer = new TableViewer(parent, SWT.MULTI | SWT.FULL_SELECTION);
		Table table = securityListViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		ColumnViewerToolTipSupport.enableFor(securityListViewer);
		// must be called  before tableViewerColumn.setLabelProvider
		securityListViewer.setLabelProvider(new SecurityListLabelProvider());

		int colNum = 0;
		TableColumn col = new TableColumn(table, SWT.LEFT);
		col.setText("Name");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 300);
		col.setResizable(true);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TableColumn)e.widget, nameComparator);
			}
		});
		TableViewerColumn tableViewerColumn = new TableViewerColumn(securityListViewer, col);
		tableViewerColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				Object element = cell.getElement();
				if (element instanceof WatchEntry) {
					WatchEntry watchEntry = (WatchEntry) element;
					Security security = watchEntry.getSecurity();
					cell.setText(security.getName());
					String icon = security.getConfigurationValue("icon");
					if (StringUtils.isBlank(icon)) {
						icon = "empty";
					}
					cell.setImage(Activator.getDefault().getImage("icons/"+icon+".png"));
				}
			}
			@Override
			public String getToolTipText(Object element) {
				if (element instanceof WatchEntry) {
					WatchEntry entry = (WatchEntry) element;
					return StringUtils.defaultString(entry.getSecurity().getConfigurationValue("iconText"));
				}
				return super.getToolTipText(element);
			}
		});
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
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TableColumn)e.widget, peRatioComparator);
			}
		});
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("P/E avg");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 100);
		col.setResizable(true);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TableColumn)e.widget, peAvgComparator);
			}
		});
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("P/E +-");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 100);
		col.setResizable(true);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TableColumn)e.widget, peDeltaComparator);
			}
		});
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Earnings +-");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 100);
		col.setResizable(true);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TableColumn)e.widget, earningsComparator);
			}
		});
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Div yield");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 100);
		col.setResizable(true);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TableColumn)e.widget, divYieldComparator);
			}
		});
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("YOC");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 100);
		col.setResizable(true);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TableColumn)e.widget, yocComparator);
			}
		});
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("D/E");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 100);
		col.setResizable(true);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TableColumn)e.widget, deRatioComparator);
			}
		});
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
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TableColumn)e.widget, WEEK_COMPARATOR);
			}
		});
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Perf. 1 month");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 100);
		col.setResizable(true);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TableColumn)e.widget, MONTH_COMPARATOR);
			}
		});
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Perf. 6 month");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 100);
		col.setResizable(true);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TableColumn)e.widget, SIX_MONTH_COMPARATOR);
			}
		});
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Perf. 1 year");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 100);
		col.setResizable(true);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TableColumn)e.widget, YEAR_COMPARATOR);
			}
		});
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Perf. 3 years");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 100);
		col.setResizable(true);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TableColumn)e.widget, THREE_YEAR_COMPARATOR);
			}
		});
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Perf. custom");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 100);
		col.setResizable(true);
		col.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSorting((TableColumn)e.widget, customPerformanceComparator);
			}
		});

		table.setSortColumn(table.getColumn(0));
		table.setSortDirection(SWT.UP);
		securityListViewer.setComparator(nameComparator);

		securityListViewer.setContentProvider(new WatchlistContentProvider());

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

		securityListViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection sel = (IStructuredSelection)event.getSelection();
				if (sel.getFirstElement() instanceof WatchEntry) {
					Security security = ((WatchEntry) sel.getFirstElement()).getSecurity();
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

		MenuManager menuManager = new MenuManager();
		table.setMenu(menuManager.createContextMenu(table));
		getSite().registerContextMenu(menuManager, securityListViewer);
		getSite().setSelectionProvider(securityListViewer);

		IActionBars actionBars = getViewSite().getActionBars();
		actionBars.setGlobalActionHandler(ActionFactory.PROPERTIES.getId(),
			new PropertyDialogAction(getSite(), securityListViewer));
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
		securityListViewer.getTable().setFocus();
	}

	protected void setSorting(TableColumn column, WatchEntryViewerComparator newComparator) {
		Table table = securityListViewer.getTable();
		if (securityListViewer.getComparator() == newComparator) {
			int currentSortDirection = table.getSortDirection();
			table.setSortDirection(currentSortDirection==SWT.UP?SWT.DOWN:SWT.UP);
			newComparator.setSortDirection(currentSortDirection==SWT.UP?WatchEntryViewerComparator.SORT.DOWN:WatchEntryViewerComparator.SORT.UP);
			securityListViewer.refresh();
		} else {
			table.setSortColumn(column);
			table.setSortDirection(SWT.UP);
			newComparator.setSortDirection(WatchEntryViewerComparator.SORT.UP);
			securityListViewer.setComparator(newComparator);
		}
	}

}
