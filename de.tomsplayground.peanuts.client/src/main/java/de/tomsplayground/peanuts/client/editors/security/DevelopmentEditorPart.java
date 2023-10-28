package de.tomsplayground.peanuts.client.editors.security;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.widgets.PersistentColumWidth;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.domain.process.StopLoss;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class DevelopmentEditorPart extends EditorPart {

	private TableViewer tableViewer;

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
			if (columnIndex < strings.length) {
				return strings[columnIndex];
			} else {
				return "";
			}
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			return null;
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			String[] strings = (String[]) element;
			if (columnIndex < strings.length) {
				String v = strings[columnIndex];
				if (columnIndex > 0 && v.startsWith("-")) {
					return red;
				}
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
		table.setFont(Activator.getDefault().getNormalFont());

		TableColumn col = new TableColumn(table, SWT.LEFT);
		col.setText("Text");
		col.setWidth(100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Start");
		col.setWidth(100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Change");
		col.setWidth(100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Change %");
		col.setWidth(100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Anual change %");
		col.setWidth(100);
		col.setResizable(true);
		
		new PersistentColumWidth(table, Activator.getDefault().getPreferenceStore(), 
				getClass().getCanonicalName()+"."+getEditorInput().getName());

		Color red = Activator.getDefault().getColorProvider().get(Activator.RED);
		tableViewer.setLabelProvider(new StringTableLabelProvider(red));
		tableViewer.setContentProvider(new ArrayContentProvider());
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		tableViewer.setInput(generateValues());
	}

	protected List<String[]> generateValues() {
		List<String[]> result = new ArrayList<String[]>();
		Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();
		IPriceProvider prices = PriceProviderFactory.getInstance().getSplitAdjustedPriceProvider(security, 
				Activator.getDefault().getAccountManager().getStockSplits(security));

		development(result, prices, ChronoUnit.DAYS, -7, "One week");
		development(result, prices, ChronoUnit.MONTHS, -1, "One month");
		development(result, prices, ChronoUnit.MONTHS, -3, "Three months");
		development(result, prices, ChronoUnit.MONTHS, -6, "Six months");
		development(result, prices, ChronoUnit.DAYS, -LocalDate.now().getDayOfYear(), "This year");
		development(result, prices, ChronoUnit.YEARS, -1, "One year");
		development(result, prices, ChronoUnit.YEARS, -3, "Three years");
		development(result, prices, ChronoUnit.YEARS, -5, "Five years");
		development(result, prices, ChronoUnit.YEARS, -7, "Seven years");
		development(result, prices, ChronoUnit.YEARS, -10, "Ten years");

		ImmutableSet<StopLoss> stopLosses = Activator.getDefault().getAccountManager().getStopLosses(security);
		if (! stopLosses.isEmpty()) {
			ImmutableList<Price> stopPrices = stopLosses.iterator().next().getPrices(prices);
			if (! stopPrices.isEmpty()) {
				BigDecimal stopLoss = stopPrices.get(stopPrices.size()-1).getValue();
				IPrice price = prices.getPrice(prices.getMaxDate());
				if (price.getValue().compareTo(BigDecimal.ZERO) != 0) {
					BigDecimal delta = price.getValue().subtract(stopLoss);
					BigDecimal percent = delta.divide(price.getValue(), PeanutsUtil.MC);
					percent = percent.movePointRight(2);
					percent = percent.setScale(2, RoundingMode.HALF_UP);
					result.add(new String[]{"Stop Loss", PeanutsUtil.formatCurrency(stopLoss, null),
						PeanutsUtil.formatCurrency(delta, null), percent.toString()+" %"});
				} else {
					result.add(new String[]{"Stop Loss", PeanutsUtil.formatCurrency(stopLoss, null)});
				}
			}
		}

		return result;
	}

	private void development(List<String[]> result, IPriceProvider prices, TemporalUnit field, int delta, String text) {
		LocalDate date = LocalDate.now();
		Day to = Day.from(date);
		date = date.plus(delta, field);
		Day from = Day.from(date);
		IPrice price1 = prices.getPrice(from);
		IPrice price2 = prices.getPrice(to);
		BigDecimal diff = price2.getValue().subtract(price1.getValue());
		if (price1.getValue().compareTo(BigDecimal.ZERO) != 0 && from.delta(to) > 0) {
			BigDecimal anualDiff = price2.getValue().divide(price1.getValue(), PeanutsUtil.MC);
			anualDiff = new BigDecimal(Math.pow(anualDiff.doubleValue(), 360.0 / from.delta(to))).subtract(BigDecimal.ONE);
			anualDiff = anualDiff.movePointRight(2).setScale(2, RoundingMode.HALF_UP);

			BigDecimal diffPercent = diff.divide(price1.getValue(), PeanutsUtil.MC);
			diffPercent = diffPercent.movePointRight(2).setScale(2, RoundingMode.HALF_UP);
			result.add(new String[]{text, PeanutsUtil.formatCurrency(price1.getValue(), null), PeanutsUtil.formatCurrency(diff, null), diffPercent.toString()+" %", anualDiff.toString()+" %"});
		} else {
			result.add(new String[]{text, PeanutsUtil.formatCurrency(price1.getValue(), null), PeanutsUtil.formatCurrency(diff, null), "-", "-"});
		}
	}

	@Override
	public void setFocus() {
		tableViewer.getTable().setFocus();
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
