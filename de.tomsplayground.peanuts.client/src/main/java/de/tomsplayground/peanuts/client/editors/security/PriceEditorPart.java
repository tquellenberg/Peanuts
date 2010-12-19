package de.tomsplayground.peanuts.client.editors.security;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableEditor;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import de.tomsplayground.peanuts.client.util.UniqueAsyncExecution;
import de.tomsplayground.peanuts.client.widgets.DateCellEditor;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.util.PeanutsUtil;
import de.tomsplayground.util.Day;

public class PriceEditorPart extends EditorPart implements IPersistableEditor {

	private TableViewer tableViewer;
	private int colWidth[] = new int[5];
	private boolean dirty = false;
	private IPriceProvider priceProvider;

	private PropertyChangeListener priceProviderChangeListener = new UniqueAsyncExecution() {

		@Override
		public void doit(PropertyChangeEvent evt, Display display) {
			if (! tableViewer.getControl().isDisposed()) {
				// Full update
				List<Price> list = priceProvider.getPrices();
				Collections.reverse(list);
				tableViewer.setInput(list);						
			}
		}

		@Override
		public Display getDisplay() {
			return getSite().getWorkbenchWindow().getWorkbench().getDisplay();
		}
	};

	private static class PriceTableLabelProvider extends LabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			Price price = (Price) element;
			switch (columnIndex) {
			case 0:
				return PeanutsUtil.formatDate(price.getDay());
			case 1:
				return PeanutsUtil.formatCurrency(price.getOpen(), null);
			case 2:
				return PeanutsUtil.formatCurrency(price.getClose(), null);
			case 3:
				return PeanutsUtil.formatCurrency(price.getLow(), null);
			case 4:
				return PeanutsUtil.formatCurrency(price.getHigh(), null);
			default:
				return "";
			}
		}
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (!(input instanceof SecurityEditorInput)) {
			throw new PartInitException("Invalid Input: Must be SecurityEditorInput");
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

		tableViewer = new TableViewer(top, SWT.FULL_SELECTION);
		Table table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		TableColumn col = new TableColumn(table, SWT.LEFT);
		col.setText("Date");
		col.setWidth((colWidth[0] > 0) ? colWidth[0] : 100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.LEFT);
		col.setText("Open");
		col.setWidth((colWidth[1] > 0) ? colWidth[1] : 100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.LEFT);
		col.setText("Close");
		col.setWidth((colWidth[2] > 0) ? colWidth[2] : 100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.LEFT);
		col.setText("Low");
		col.setWidth((colWidth[3] > 0) ? colWidth[3] : 100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.LEFT);
		col.setText("High");
		col.setWidth((colWidth[4] > 0) ? colWidth[4] : 100);
		col.setResizable(true);

		tableViewer.setColumnProperties(new String[] { "date", "open", "close", "low", "high" });
		tableViewer.setCellModifier(new ICellModifier() {

			@Override
			public boolean canModify(Object element, String property) {
				return true;
			}

			private BigDecimal getValueByProperty(Price p, String property) {
				BigDecimal v = null;
				if (property.equals("open"))
					v = p.getOpen();
				else if (property.equals("close"))
					v = p.getClose();
				else if (property.equals("low"))
					v = p.getLow();
				else if (property.equals("high"))
					v = p.getHigh();
				if (v == null)
					v = BigDecimal.ZERO;
				return v;
			}

			@Override
			public Object getValue(Object element, String property) {
				Price p = (Price) element;
				if (property.equals("date"))
					return p.getDay();
				BigDecimal v = getValueByProperty(p, property);
				return PeanutsUtil.formatCurrency(v, null);
			}

			@Override
			public void modify(Object element, String property, Object value) {
				Price p = (Price) ((TableItem) element).getData();
				Price newPrice = null;
				if (property.equals("date")) {
					newPrice = new Price((Day) value, p.getOpen(), p.getClose(), p.getLow(), p.getHigh());
				} else {
					try {
						BigDecimal v = PeanutsUtil.parseCurrency((String) value);
						BigDecimal oldV = getValueByProperty(p, property);
						if (oldV.compareTo(v) != 0) {
							if (property.equals("open"))
								newPrice = new Price(p.getDay(), v, p.getClose(), p.getLow(), p.getHigh());
							else if (property.equals("close"))
								newPrice = new Price(p.getDay(), p.getOpen(), v, p.getLow(), p.getHigh());
							else if (property.equals("low"))
								newPrice = new Price(p.getDay(), p.getOpen(), p.getClose(), p.getHigh(), v);
							else if (property.equals("high"))
								newPrice = new Price(p.getDay(), p.getOpen(), p.getClose(), v, p.getLow());
						}
					} catch (ParseException e) {
						// Okay
					}
				}
				if (newPrice != null && ! newPrice.equals(p)) {
					priceProvider.removePrice(p.getDay());
					priceProvider.setPrice(newPrice);
					((TableItem) element).setData(newPrice);
					dirty = true;
					firePropertyChange(IEditorPart.PROP_DIRTY);
				}
			}
		});
		tableViewer.setCellEditors(new CellEditor[] {new DateCellEditor(table), new TextCellEditor(table),
				new TextCellEditor(table), new TextCellEditor(table), new TextCellEditor(table) });

		tableViewer.setLabelProvider(new PriceTableLabelProvider());
		tableViewer.setContentProvider(new ArrayContentProvider());
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();
		priceProvider = PriceProviderFactory.getInstance().getPriceProvider(security);
		List<Price> list = priceProvider.getPrices();
		Collections.reverse(list);
		tableViewer.setInput(list);

		MenuManager menu = new MenuManager();
		menu.setRemoveAllWhenShown(true);
		menu.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});
		table.setMenu(menu.createContextMenu(table));
		
		((ObservableModelObject) priceProvider).addPropertyChangeListener(priceProviderChangeListener);
	}

	@Override
	public void dispose() {
		((ObservableModelObject) priceProvider).removePropertyChangeListener(priceProviderChangeListener);
		super.dispose();
	}

	protected void fillContextMenu(IMenuManager manager) {
		manager.add(new Action("New") {
			@Override
			public void run() {
				Day d = priceProvider.getMaxDate();
				if (d != null)
					d = d.addDays(1);
				else
					d = new Day();
				Price price = new Price(d, BigDecimal.ZERO);
				priceProvider.setPrice(price);
				dirty = true;
				PriceEditorPart.this.firePropertyChange(IEditorPart.PROP_DIRTY);
			}
		});
	}

	@Override
	public void setFocus() {
		tableViewer.getTable().setFocus();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();
		PriceProviderFactory.getInstance().saveToLocal(security, priceProvider);
		dirty = false;
	}

	@Override
	public void doSaveAs() {
		// nothing to do
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void restoreState(IMemento memento) {
		for (int i = 0; i < colWidth.length; i++) {
			Integer width = memento.getInteger("col" + i);
			if (width != null) {
				colWidth[i] = width.intValue();
			}
		}
	}

	@Override
	public void saveState(IMemento memento) {
		TableColumn[] columns = tableViewer.getTable().getColumns();
		for (int i = 0; i < columns.length; i++) {
			TableColumn tableColumn = columns[i];
			memento.putInteger("col" + i, tableColumn.getWidth());
		}
	}

}
