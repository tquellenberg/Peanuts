package de.tomsplayground.peanuts.client.editors.security;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.text.ParseException;

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
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.util.UniqueAsyncExecution;
import de.tomsplayground.peanuts.client.widgets.DateCellEditor;
import de.tomsplayground.peanuts.client.widgets.PersistentColumWidth;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class PriceEditorPart extends EditorPart {

	private TableViewer tableViewer;
	private boolean dirty = false;
	private IPriceProvider priceProvider;

	private final PropertyChangeListener priceProviderChangeListener = new UniqueAsyncExecution() {

		@Override
		public void doit(PropertyChangeEvent evt, Display display) {
			if (! tableViewer.getControl().isDisposed()) {
				// Full update
				ImmutableList<IPrice> list = priceProvider.getPrices();
				tableViewer.setInput(list.reverse());
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
					return PeanutsUtil.formatCurrency(price.getValue(), null);
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
		table.setFont(Activator.getDefault().getNormalFont());

		TableColumn col = new TableColumn(table, SWT.LEFT);
		col.setText("Date");
		col.setWidth(100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.LEFT);
		col.setText("Close");
		col.setWidth(100);
		col.setResizable(true);

		new PersistentColumWidth(table, Activator.getDefault().getPreferenceStore(), 
				getClass().getCanonicalName()+"."+getEditorInput().getName());
		
		tableViewer.setColumnProperties(new String[] { "date", "close"});
		tableViewer.setCellModifier(new ICellModifier() {

			@Override
			public boolean canModify(Object element, String property) {
				return true;
			}

			private BigDecimal getValueByProperty(Price p, String property) {
				BigDecimal v = null;
				if (property.equals("close")) {
					v = p.getValue();
				}
				if (v == null) {
					v = BigDecimal.ZERO;
				}
				return v;
			}

			@Override
			public Object getValue(Object element, String property) {
				Price p = (Price) element;
				if (property.equals("date")) {
					return p.getDay();
				}
				BigDecimal v = getValueByProperty(p, property);
				return PeanutsUtil.formatCurrency(v, null);
			}

			@Override
			public void modify(Object element, String property, Object value) {
				if (element == null) {
					return;
				}
				Price p = (Price) ((TableItem) element).getData();
				Price newPrice = null;
				if (property.equals("date")) {
					newPrice = new Price((Day) value, p.getValue());
				} else {
					try {
						BigDecimal v = PeanutsUtil.parseCurrency((String) value);
						BigDecimal oldV = getValueByProperty(p, property);
						if (oldV.compareTo(v) != 0) {
							if (property.equals("close")) {
								newPrice = new Price(p.getDay(), v);
							}
						}
					} catch (ParseException e) {
						// Okay
					}
				}
				if (newPrice != null && ! newPrice.equals(p)) {
					priceProvider.removePrice(p.getDay());
					priceProvider.setPrice(newPrice);
					((TableItem) element).setData(newPrice);
					markDirty();
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
		ImmutableList<IPrice> list = priceProvider.getPrices();
		tableViewer.setInput(list.reverse());

		MenuManager menuManager = new MenuManager();
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});
		table.setMenu(menuManager.createContextMenu(table));
		getSite().registerContextMenu(menuManager, tableViewer);
		getSite().setSelectionProvider(tableViewer);

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
				if (d != null) {
					d = d.addDays(1);
				} else {
					d = Day.today();
				}
				Price price = new Price(d, BigDecimal.ZERO);
				priceProvider.setPrice(price);
				markDirty();
			}
		});
	}

	@Override
	public void setFocus() {
		tableViewer.getTable().setFocus();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		if (dirty) {
			Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();
			PriceProviderFactory.getInstance().saveToLocal(security, priceProvider);
			dirty = false;
		}
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

	public void markDirty() {
		dirty = true;
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}

}
