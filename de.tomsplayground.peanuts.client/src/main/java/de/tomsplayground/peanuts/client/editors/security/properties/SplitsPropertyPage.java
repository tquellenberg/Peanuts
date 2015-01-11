package de.tomsplayground.peanuts.client.editors.security.properties;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

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

import com.google.common.collect.Lists;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.widgets.DateCellEditor;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.StockSplit;
import de.tomsplayground.peanuts.util.PeanutsUtil;
import de.tomsplayground.util.Day;

public class SplitsPropertyPage extends PropertyPage {

	private List<StockSplit> stockSplits;
	private TableViewer tableViewer;
	private Security security;

	private static class SplitTableLabelProvider extends LabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			StockSplit split = (StockSplit) element;
			switch (columnIndex) {
				case 0:
					return PeanutsUtil.formatDate(split.getDay());
				case 1:
					return String.valueOf(split.getFrom());
				case 2:
					return String.valueOf(split.getTo());
				default:
					return "";
			}
		}
	}

	public SplitsPropertyPage() {
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
		col.setText("From");
		col.setWidth(100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.LEFT);
		col.setText("To");
		col.setWidth(100);
		col.setResizable(true);

		tableViewer.setColumnProperties(new String[] { "date", "from", "to" });
		tableViewer.setCellModifier(new ICellModifier() {

			@Override
			public boolean canModify(Object element, String property) {
				return true;
			}

			private Integer getValueByProperty(StockSplit p, String property) {
				int v = 0;
				if (property.equals("from")) {
					v = p.getFrom();
				} else if (property.equals("to")) {
					v = p.getTo();
				}
				return Integer.valueOf(v);
			}

			@Override
			public Object getValue(Object element, String property) {
				StockSplit p = (StockSplit) element;
				if (property.equals("date")) {
					return p.getDay();
				}
				return String.valueOf(getValueByProperty(p, property));
			}

			@Override
			public void modify(Object element, String property, Object value) {
				StockSplit p = (StockSplit) ((TableItem) element).getData();
				StockSplit newP = null;
				if (property.equals("date")) {
					newP = new StockSplit(security, (Day) value, p.getFrom(), p.getTo());
				} else {
					try {
						int v = Integer.parseInt((String) value);
						Integer oldV = getValueByProperty(p, property);
						if (oldV.intValue() != v) {
							if (property.equals("from")) {
								newP = new StockSplit(security, p.getDay(), v, p.getTo());
							} else if (property.equals("to")) {
								newP = new StockSplit(security, p.getDay(), p.getFrom(), v);
							}
						}
					} catch (NumberFormatException e) {
						// Okay
					}
				}
				if (newP != null) {
					stockSplits.set(stockSplits.indexOf(p), newP);
					tableViewer.refresh();
				}
			}
		});
		tableViewer.setCellEditors(new CellEditor[] {new DateCellEditor(table), new TextCellEditor(table), new TextCellEditor(table) });

		tableViewer.setLabelProvider(new SplitTableLabelProvider());
		tableViewer.setContentProvider(new ArrayContentProvider());
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		stockSplits = Lists.newArrayList(Activator.getDefault().getAccountManager().getStockSplits(security));
		tableViewer.setInput(stockSplits);

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
				StockSplit stockSplit = new StockSplit(security, new Day(), 0, 0);
				stockSplits.add(stockSplit);
				tableViewer.refresh();
			}
		});
		final Action deleteAction = new Action("Delete") {
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				IStructuredSelection sel = (IStructuredSelection)tableViewer.getSelection();
				if (! sel.isEmpty()) {
					for (Iterator<StockSplit> iter = sel.iterator(); iter.hasNext(); ) {
						StockSplit stockSplit = iter.next();
						stockSplits.remove(stockSplit);
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
		accountManager.setStockSplits(security, new HashSet<StockSplit>(stockSplits));
		return true;
	}

}
