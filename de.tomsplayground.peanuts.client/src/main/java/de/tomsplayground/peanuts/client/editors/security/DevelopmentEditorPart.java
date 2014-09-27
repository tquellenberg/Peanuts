package de.tomsplayground.peanuts.client.editors.security;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableEditor;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.util.PeanutsUtil;
import de.tomsplayground.util.Day;

public class DevelopmentEditorPart extends EditorPart implements IPersistableEditor {

	private TableViewer tableViewer;
	private final int colWidth[] = new int[3];

	private static class StringTableLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider {

		private final Color red;

		public StringTableLabelProvider(Color red) {
			this.red = red;
		}
		
		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			String[] strings = (String[]) element;
			return strings[columnIndex];
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			return null;
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			String v = ((String[]) element)[columnIndex];
			if (columnIndex > 0 && v.startsWith("-")) {
				return red;
			}
			return null;
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

		tableViewer = new TableViewer(top, SWT.NONE);
		Table table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		TableColumn col = new TableColumn(table, SWT.LEFT);
		col.setText("Text");
		col.setWidth((colWidth[0] > 0) ? colWidth[0] : 100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Value");
		col.setWidth((colWidth[1] > 0) ? colWidth[1] : 100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Percent");
		col.setWidth((colWidth[2] > 0) ? colWidth[2] : 100);
		col.setResizable(true);

		Color red = Activator.getDefault().getColorProvider().get(Activator.RED);
		tableViewer.setLabelProvider(new StringTableLabelProvider(red));
		tableViewer.setContentProvider(new ArrayContentProvider());
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		tableViewer.setInput(generateValues());
	}
	
	protected List<String[]> generateValues() {
		List<String[]> result = new ArrayList<String[]>();
		Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();
		IPriceProvider prices = PriceProviderFactory.getInstance().getPriceProvider(security);

		development(result, prices, Calendar.DAY_OF_MONTH, -7, "One week");
		development(result, prices, Calendar.MONTH, -1, "One month");
		development(result, prices, Calendar.MONTH, -3, "Three months");
		development(result, prices, Calendar.MONTH, -6, "Six months");
		development(result, prices, Calendar.YEAR, -1, "One year");
		development(result, prices, Calendar.YEAR, -3, "Three years");
		development(result, prices, Calendar.YEAR, -5, "Five years");
		
		String stopLossValue = security.getConfigurationValue("STOPLOSS");
		if (StringUtils.isNotEmpty(stopLossValue)) {
			try {
				BigDecimal stopLoss = PeanutsUtil.parseQuantity(stopLossValue);
				IPrice price = prices.getPrice(prices.getMaxDate());
				if (price.getValue().compareTo(BigDecimal.ZERO) != 0) {
					BigDecimal percent = stopLoss.divide(price.getValue(), new MathContext(10, RoundingMode.HALF_EVEN));
					percent = percent.subtract(BigDecimal.ONE);
					percent = percent.movePointRight(2);
					percent = percent.setScale(2, RoundingMode.HALF_EVEN);
					result.add(new String[]{"Stop Loss", PeanutsUtil.formatCurrency(stopLoss, null), percent.toString()+" %"});
				} else {
					result.add(new String[]{"Stop Loss", PeanutsUtil.formatCurrency(stopLoss, null), "NaN"});
				}
			} catch (ParseException e) {
				// Okay
			}
		}
		
		return result;
	}

	private void development(List<String[]> result, IPriceProvider prices, int field, int delta, String text) {
		Calendar date = Calendar.getInstance();
		Day to = Day.fromCalendar(date);
		date.add(field, delta);
		Day from = Day.fromCalendar(date);
		IPrice price1 = prices.getPrice(from);
		IPrice price2 = prices.getPrice(to);
		BigDecimal diff = price2.getValue().subtract(price1.getValue());
		if (price1.getValue().compareTo(BigDecimal.ZERO) != 0) {
			BigDecimal diffPercent = diff.divide(price1.getValue(), new MathContext(10, RoundingMode.HALF_EVEN)).movePointRight(2);
			diffPercent = diffPercent.setScale(2, RoundingMode.HALF_EVEN);
			result.add(new String[]{text, PeanutsUtil.formatCurrency(diff, null), diffPercent.toString()+" %"});
		} else {
			result.add(new String[]{text, PeanutsUtil.formatCurrency(diff, null), "NaN"});
		}
	}

	@Override
	public void setFocus() {
		tableViewer.getTable().setFocus();
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

	@Override
	public void doSave(IProgressMonitor monitor) {
		// nothing to do
	}

	@Override
	public void doSaveAs() {
		// nothing to do
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

}
