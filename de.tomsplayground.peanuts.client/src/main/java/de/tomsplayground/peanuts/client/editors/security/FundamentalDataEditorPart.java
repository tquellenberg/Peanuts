package de.tomsplayground.peanuts.client.editors.security;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
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
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.google.common.collect.Lists;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.base.InventoryEntry;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.fundamental.FundamentalData;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.util.PeanutsUtil;
import de.tomsplayground.util.Day;

public class FundamentalDataEditorPart extends EditorPart {

	private TableViewer tableViewer;
	private final int colWidth[] = new int[6];
	private boolean dirty = false;
	private List<FundamentalData> fundamentalDatas;
	private IPriceProvider priceProvider;
	private InventoryEntry inventoryEntry;

	private class FundamentalDataTableLabelProvider extends LabelProvider implements ITableLabelProvider {
		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			FundamentalData data = (FundamentalData) element;
			switch (columnIndex) {
			case 0:
				return String.valueOf(data.getYear());
			case 1:
				return PeanutsUtil.formatCurrency(data.getDividende(), null);
			case 2:
				return PeanutsUtil.formatCurrency(data.getEarningsPerShare(), null);
			case 3:
				return PeanutsUtil.formatQuantity(data.calculatePeRatio(priceProvider));
			case 4:
				return PeanutsUtil.formatPercent(data.calculateDivYield(priceProvider));
			case 5:
				if (inventoryEntry != null && data.getYear() == (new Day()).year) {
					return PeanutsUtil.formatPercent(data.calculateYOC(inventoryEntry));
				} else {
					return "";
				}
			default:
				return "";
			}
		}
		@Override
		public String getText(Object element) {
			FundamentalData data = (FundamentalData) element;
			return String.valueOf(data.getYear());
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
		col.setText("Year");
		col.setWidth((colWidth[0] > 0) ? colWidth[0] : 100);
		col.setResizable(true);
		ViewerComparator comparator = new ViewerComparator() {
			@Override
			public boolean isSorterProperty(Object element, String property) {
				return "year".equals(property);
			}
		};
		tableViewer.setComparator(comparator);
		table.setSortColumn(col);
		table.setSortDirection(SWT.UP);

		col = new TableColumn(table, SWT.LEFT);
		col.setText("Dividende");
		col.setWidth((colWidth[1] > 0) ? colWidth[1] : 100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.LEFT);
		col.setText("EPS");
		col.setWidth((colWidth[2] > 0) ? colWidth[2] : 100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.LEFT);
		col.setText("P/E ratio");
		col.setWidth((colWidth[3] > 0) ? colWidth[3] : 100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.LEFT);
		col.setText("Div yield");
		col.setWidth((colWidth[4] > 0) ? colWidth[4] : 100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.LEFT);
		col.setText("YOC");
		col.setWidth((colWidth[5] > 0) ? colWidth[5] : 100);
		col.setResizable(true);

		tableViewer.setColumnProperties(new String[] { "year", "div", "EPS", "peRatio", "divYield", "YOC"});
		tableViewer.setCellModifier(new ICellModifier() {

			@Override
			public boolean canModify(Object element, String property) {
				return Lists.newArrayList("year", "div", "EPS").contains(property);
			}

			@Override
			public Object getValue(Object element, String property) {
				FundamentalData p = (FundamentalData) element;
				if (property.equals("year")) {
					return String.valueOf(p.getYear());
				} else if (property.equals("div")) {
					return PeanutsUtil.formatCurrency(p.getDividende(), null);
				} else if (property.equals("EPS")) {
					return PeanutsUtil.formatCurrency(p.getEarningsPerShare(), null);
				}
				return null;
			}

			@Override
			public void modify(Object element, String property, Object value) {
				FundamentalData p = (FundamentalData) ((TableItem) element).getData();
				try {
					if (property.equals("year")) {
						Integer newYear = Integer.valueOf((String) value);
						if (newYear.intValue() != p.getYear()) {
							p.setYear(newYear.intValue());
							tableViewer.update(p, new String[]{property});
							markDirty();
						}
					} else if (property.equals("div")) {
						BigDecimal v = PeanutsUtil.parseCurrency((String) value);
						if (! v.equals(p.getDividende())) {
							p.setDividende(v);
							tableViewer.update(p, new String[]{property});
							markDirty();
						}
					} else if (property.equals("EPS")) {
						BigDecimal v = PeanutsUtil.parseCurrency((String) value);
						if (! v.equals(p.getEarningsPerShare())) {
							p.setEarningsPerShare(v);
							tableViewer.update(p, new String[]{property});
							markDirty();
						}
					}
				} catch (ParseException e) {
					// Okay
				}
			}
		});
		tableViewer.setCellEditors(new CellEditor[] {new TextCellEditor(table), new TextCellEditor(table),
				new TextCellEditor(table)});

		tableViewer.setLabelProvider(new FundamentalDataTableLabelProvider());
		tableViewer.setContentProvider(new ArrayContentProvider());
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();
		priceProvider = PriceProviderFactory.getInstance().getPriceProvider(security);
		Inventory inventory = Activator.getDefault().getAccountManager().getFullInventory();
		for (InventoryEntry entry : inventory.getEntries()) {
			if (entry.getSecurity().equals(security)) {
				inventoryEntry = entry;
			}
		}
		
		fundamentalDatas = cloneFundamentalData(security.getFundamentalDatas());
		tableViewer.setInput(fundamentalDatas);

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
	}

	private List<FundamentalData> cloneFundamentalData(Collection<FundamentalData> datas) {
		List<FundamentalData> fundamentalDatas = new ArrayList<FundamentalData>();
		for (FundamentalData d : datas) {
			fundamentalDatas.add(new FundamentalData(d));
		}
		return fundamentalDatas;
	}
	
	@Override
	public void setFocus() {
		tableViewer.getTable().setFocus();
	}

	protected void fillContextMenu(IMenuManager manager) {
		manager.add(new Action("New") {
			@Override
			public void run() {
				FundamentalData fundamentalData = new FundamentalData();
				fundamentalDatas.add(fundamentalData);
				tableViewer.add(fundamentalData);
				markDirty();
			}
		});
	}

	public void deleteFundamentalData(Collection<FundamentalData> data) {
		if (fundamentalDatas.removeAll(data)) {
			tableViewer.remove(data.toArray());
			markDirty();
		}
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();
		security.setFundamentalDatas(fundamentalDatas);
		dirty = false;
	}

	@Override
	public void doSaveAs() {
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
