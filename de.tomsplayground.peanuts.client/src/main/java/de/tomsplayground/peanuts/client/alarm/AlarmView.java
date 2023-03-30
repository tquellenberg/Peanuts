package de.tomsplayground.peanuts.client.alarm;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.editors.security.SecurityEditor;
import de.tomsplayground.peanuts.client.editors.security.SecurityEditorInput;
import de.tomsplayground.peanuts.client.util.UniqueAsyncExecution;
import de.tomsplayground.peanuts.domain.alarm.AlarmManager;
import de.tomsplayground.peanuts.domain.alarm.SecurityAlarm;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class AlarmView extends ViewPart {

	public static final String ID = "de.tomsplayground.peanuts.client.alarmView";

	private TableViewer alarmListViewer;

	private final int colWidth[] = new int[4];

	private final AlarmManager alarmManager = new AlarmManager(PriceProviderFactory.getInstance());

	private ScheduledExecutorService executor;

	private final PropertyChangeListener propertyChangeListener = new UniqueAsyncExecution() {
		@Override
		public void doit(PropertyChangeEvent evt, Display display) {
			alarmListViewer.setInput(getSecurityAlarms());
			alarmListViewer.refresh();
			executor.submit(AlarmView.this::checkAlarms);
		}

		@Override
		public Display getDisplay() {
			return getSite().getShell().getDisplay();
		}
	};

	private class AlarmListLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider {
		@Override
		public String getColumnText(Object element, int columnIndex) {
			SecurityAlarm e = (SecurityAlarm)element;
			if (columnIndex == 0) {
				return e.getSecurity().getName();
			} else if (columnIndex == 1) {
				return e.getMode().name();
			} else if (columnIndex == 2) {
				return PeanutsUtil.formatCurrency(e.getValue(), e.getSecurity().getCurrency());
			} else if (columnIndex == 3) {
				return PeanutsUtil.formatDate(e.getTriggerDay());
			}
			return null;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			SecurityAlarm e = (SecurityAlarm)element;
			if (e.isTriggered()) {
				return Activator.getDefault().getColorProvider().get(Activator.ACTIVE_ROW);
			} else {
				return Activator.getDefault().getColorProvider().get(Activator.INACTIVE_ROW);
			}
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			return null;
		}
	}

	private class AlarmListViewerComparator extends ViewerComparator {
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (e1 instanceof SecurityAlarm w1 && e2 instanceof SecurityAlarm w2) {
				if (w1.isTriggered() && !w2.isTriggered()) {
					return -1;
				}
				if (! w1.isTriggered() && w2.isTriggered()) {
					return 1;
				}
				Day t1 = w1.getTriggerDay();
				Day t2 = w2.getTriggerDay();
				if (t1 != null && t2 != null && ! t1.equals(t2)) {
					return t2.compareTo(t1);
				}
				return w1.getSecurity().getName().compareTo(w2.getSecurity().getName());
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
		executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("Alarm-Checker-%d").build());
		executor.scheduleWithFixedDelay(this::checkAlarms, 0, 1, TimeUnit.MINUTES);
	}

	private AtomicLong alarmsTriggeredToday = new AtomicLong();
	
	private void checkAlarms() {
		alarmManager.checkAlarms(Activator.getDefault().getAccountManager().getSecurityAlarms());
		List<SecurityAlarm> securityAlarms = getSecurityAlarms();
		long count = securityAlarms.stream()
				.filter(a -> a.isTriggered() && a.getTriggerDay().equals(Day.today()))
				.count();
		if (alarmsTriggeredToday.longValue() != count) {
			getSite().getShell().getDisplay().asyncExec(() -> {
				alarmListViewer.setInput(securityAlarms);
				alarmListViewer.refresh();
			});
			alarmsTriggeredToday.set(count);
		}
	}

	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		TableColumn[] columns = alarmListViewer.getTable().getColumns();
		for (int i = 0; i < columns.length; i++ ) {
			TableColumn tableColumn = columns[i];
			memento.putInteger("col" + i, tableColumn.getWidth());
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		alarmListViewer = new TableViewer(parent, SWT.MULTI | SWT.FULL_SELECTION);
		Table table = alarmListViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setFont(Activator.getDefault().getNormalFont());
		ColumnViewerToolTipSupport.enableFor(alarmListViewer);
		// must be called  before tableViewerColumn.setLabelProvider
		alarmListViewer.setLabelProvider(new AlarmListLabelProvider());

		int colNum = 0;
		TableColumn col = new TableColumn(table, SWT.LEFT);
		col.setText("Security");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 300);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.LEFT);
		col.setText("Mode");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 150);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.LEFT);
		col.setText("Value");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 400);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.LEFT);
		col.setText("Trigger");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 400);
		col.setResizable(true);
		colNum++;

		Activator.getDefault().getAccountManager().addPropertyChangeListener("securityAlarm", propertyChangeListener);

		alarmListViewer.setComparator(new AlarmListViewerComparator());
		alarmListViewer.setContentProvider(new ArrayContentProvider());
		alarmListViewer.setInput(getSecurityAlarms());

		alarmListViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection sel = (IStructuredSelection)event.getSelection();
				if (sel.getFirstElement() instanceof SecurityAlarm secAlarm) {
					Security security = secAlarm.getSecurity();
					IEditorInput input = new SecurityEditorInput(security);
					try {
						getSite().getWorkbenchWindow().getActivePage().openEditor(input, SecurityEditor.ID);
					} catch (PartInitException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}
	
	private List<SecurityAlarm> getSecurityAlarms() {
		AccountManager accountManager = Activator.getDefault().getAccountManager();
		ImmutableList<SecurityAlarm> securityAlarms = Activator.getDefault().getAccountManager().getSecurityAlarms();
		List<Security> securities = accountManager.getSecurities().stream()
			.filter(s -> !s.isDeleted())
			.toList();
		List<SecurityAlarm> highLowAlarms = alarmManager.checkHighLow(securities, accountManager);
		highLowAlarms.addAll(securityAlarms);
		
		return highLowAlarms;
	}

	@Override
	public void dispose() {
		Activator.getDefault().getAccountManager().removePropertyChangeListener("securityAlarm", propertyChangeListener);
		executor.shutdown();
	}

	@Override
	public void setFocus() {
		alarmListViewer.getTable().setFocus();
	}

}
