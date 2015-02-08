package de.tomsplayground.peanuts.client.editors.security.properties;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.dialogs.PropertyPage;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.widgets.DateCellEditor;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.calendar.SecurityCalendarEntry;
import de.tomsplayground.peanuts.util.PeanutsUtil;
import de.tomsplayground.util.Day;

public class CalendarPropertyPage extends PropertyPage {

	private List<SecurityCalendarEntry> securityCalendarEntry;
	private TableViewer tableViewer;
	private Security security;

	private static class SecurityCalendarEntryTableLabelProvider extends LabelProvider implements ITableLabelProvider {
		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			SecurityCalendarEntry entry = (SecurityCalendarEntry) element;
			switch (columnIndex) {
				case 0:
					return PeanutsUtil.formatDate(entry.getDay());
				case 1:
					return entry.getName();
				default:
					return "";
			}
		}
	}

	public CalendarPropertyPage() {
		noDefaultAndApplyButton();
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent,SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);

		IAdaptable adapter = getElement();
		security = (Security)adapter.getAdapter(Security.class);

		tableViewer = new TableViewer(composite, SWT.NONE);
		Table table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		TableColumn col = new TableColumn(table, SWT.LEFT);
		col.setText("Date");
		col.setWidth(100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.LEFT);
		col.setText("Name");
		col.setWidth(250);
		col.setResizable(true);

		tableViewer.setColumnProperties(new String[] { "date", "name" });
		tableViewer.setCellModifier(new ICellModifier() {

			@Override
			public boolean canModify(Object element, String property) {
				return true;
			}

			private String getValueByProperty(SecurityCalendarEntry p, String property) {
				String v = "";
				if (property.equals("name")) {
					v = p.getName();
				}
				return v;
			}

			@Override
			public Object getValue(Object element, String property) {
				SecurityCalendarEntry p = (SecurityCalendarEntry) element;
				if (property.equals("date")) {
					return p.getDay();
				}
				return getValueByProperty(p, property);
			}

			@Override
			public void modify(Object element, String property, Object value) {
				SecurityCalendarEntry p = (SecurityCalendarEntry) ((TableItem) element).getData();
				SecurityCalendarEntry newP = null;
				if (property.equals("date")) {
					newP = new SecurityCalendarEntry(security, (Day) value, p.getName());
				} else {
					String oldV = getValueByProperty(p, property);
					if (! StringUtils.equals(oldV, (String) value)) {
						if (property.equals("name")) {
							newP = new SecurityCalendarEntry(security, p.getDay(), (String)value);
						}
					}
				}
				if (newP != null) {
					securityCalendarEntry.set(securityCalendarEntry.indexOf(p), newP);
					tableViewer.refresh();
				}
			}
		});
		tableViewer.setCellEditors(new CellEditor[] {new DateCellEditor(table), new TextCellEditor(table) });

		tableViewer.setLabelProvider(new SecurityCalendarEntryTableLabelProvider());
		tableViewer.setContentProvider(new ArrayContentProvider());
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		securityCalendarEntry = Lists.newArrayList(Activator.getDefault().getAccountManager().getCalendarEntries(security));
		tableViewer.setInput(securityCalendarEntry);

		MenuManager menu = new MenuManager();
		menu.setRemoveAllWhenShown(true);
		menu.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});
		table.setMenu(menu.createContextMenu(table));

		return composite;
	}

	protected void fillContextMenu(IMenuManager manager) {
		manager.add(new Action("New") {
			@Override
			public void run() {
				SecurityCalendarEntry entry = new SecurityCalendarEntry(security, new Day(), "");
				securityCalendarEntry.add(entry);
				tableViewer.refresh();
			}
		});
		final Action deleteAction = new Action("Delete") {
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				IStructuredSelection sel = (IStructuredSelection)tableViewer.getSelection();
				if (! sel.isEmpty()) {
					for (Iterator<SecurityCalendarEntry> iter = sel.iterator(); iter.hasNext(); ) {
						SecurityCalendarEntry entry = iter.next();
						securityCalendarEntry.remove(entry);
					}
					tableViewer.refresh();
				}
			}
		};
		deleteAction.setEnabled(! ((IStructuredSelection)tableViewer.getSelection()).isEmpty());
		manager.add(deleteAction);
	}

	@Override
	public boolean performOk() {
		AccountManager accountManager = Activator.getDefault().getAccountManager();
		ImmutableSet<SecurityCalendarEntry> oldEntries = accountManager.getCalendarEntries(security);
		for (SecurityCalendarEntry calendarEntry : oldEntries) {
			accountManager.removeCalendarEntry(calendarEntry);
		}
		for (SecurityCalendarEntry calendarEntry : securityCalendarEntry) {
			accountManager.addCalendarEntry(calendarEntry);
		}
		return true;
	}

}
