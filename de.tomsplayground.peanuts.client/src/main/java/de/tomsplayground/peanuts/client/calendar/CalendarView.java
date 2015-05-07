package de.tomsplayground.peanuts.client.calendar;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
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
import de.tomsplayground.peanuts.client.util.UniqueAsyncExecution;
import de.tomsplayground.peanuts.domain.calendar.CalendarEntry;
import de.tomsplayground.peanuts.domain.calendar.SecurityCalendarEntry;
import de.tomsplayground.peanuts.util.PeanutsUtil;
import de.tomsplayground.util.Day;

public class CalendarView extends ViewPart {

	public static final String ID = "de.tomsplayground.peanuts.client.calendarView";

	private TableViewer calendarListViewer;

	private final int colWidth[] = new int[3];

	private final PropertyChangeListener propertyChangeListener = new UniqueAsyncExecution() {
		@Override
		public void doit(PropertyChangeEvent evt, Display display) {
			calendarListViewer.setInput(Activator.getDefault().getAccountManager().getCalendarEntries());
			calendarListViewer.refresh();
		}

		@Override
		public Display getDisplay() {
			return getSite().getShell().getDisplay();
		}
	};

	private class CalendarListLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider {
		@Override
		public String getColumnText(Object element, int columnIndex) {
			CalendarEntry e = (CalendarEntry)element;
			if (columnIndex == 0) {
				return PeanutsUtil.formatDate(e.getDay());
			} else if (columnIndex == 1) {
				return e.getName();
			} else if (columnIndex == 2) {
				if (e instanceof SecurityCalendarEntry) {
					return ((SecurityCalendarEntry)e).getSecurity().getName();
				}
			}
			return null;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			CalendarEntry e = (CalendarEntry)element;
			int compareTo = e.getDay().compareTo(new Day());
			if (compareTo == 0) {
				return Activator.getDefault().getColorProvider().get(Activator.ACTIVE_ROW);
			} else if (compareTo < 0) {
				return Activator.getDefault().getColorProvider().get(Activator.INACTIVE_ROW);
			}
			return null;
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			return null;
		}
	}

	private class CalendarListViewerComparator extends ViewerComparator {
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (e1 instanceof CalendarEntry && e2 instanceof CalendarEntry) {
				CalendarEntry w1 = (CalendarEntry) e1;
				CalendarEntry w2 = (CalendarEntry) e2;
				int compare = w1.getDay().compareTo(w2.getDay());
				return compare;
			}
			return 0;
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
		TableColumn[] columns = calendarListViewer.getTable().getColumns();
		for (int i = 0; i < columns.length; i++ ) {
			TableColumn tableColumn = columns[i];
			memento.putInteger("col" + i, tableColumn.getWidth());
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		calendarListViewer = new TableViewer(parent, SWT.MULTI | SWT.FULL_SELECTION);
		Table table = calendarListViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		ColumnViewerToolTipSupport.enableFor(calendarListViewer);
		// must be called  before tableViewerColumn.setLabelProvider
		calendarListViewer.setLabelProvider(new CalendarListLabelProvider());

		int colNum = 0;
		TableColumn col = new TableColumn(table, SWT.LEFT);
		col.setText("Date");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 150);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.LEFT);
		col.setText("Text");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 400);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.LEFT);
		col.setText("Security");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 300);
		col.setResizable(true);
		colNum++;

		Activator.getDefault().getAccountManager().addPropertyChangeListener("calendarEntry", propertyChangeListener);

		calendarListViewer.setComparator(new CalendarListViewerComparator());
		calendarListViewer.setContentProvider(new ArrayContentProvider());
		calendarListViewer.addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				Day now = new Day();
				if (element instanceof CalendarEntry) {
					CalendarEntry ce = (CalendarEntry)element;
					return (ce.getDay().delta(now) < 14);
				}
				return true;
			}
		});
		calendarListViewer.setInput(Activator.getDefault().getAccountManager().getCalendarEntries());
	}

	@Override
	public void dispose() {
		Activator.getDefault().getAccountManager().removePropertyChangeListener("calendarEntry", propertyChangeListener);
	}

	@Override
	public void setFocus() {
		calendarListViewer.getTable().setFocus();
	}

}
