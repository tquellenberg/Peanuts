package de.tomsplayground.peanuts.client.dividend;

import java.beans.PropertyChangeListener;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
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
import de.tomsplayground.peanuts.domain.currenncy.Currencies;
import de.tomsplayground.peanuts.domain.dividend.DividendMonth;
import de.tomsplayground.peanuts.domain.dividend.DividendStats;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class DividendStatsView extends ViewPart {

	public static final String ID = "de.tomsplayground.peanuts.client.dividendStatsView";

	private TableViewer dividendStatsListViewer;

	private final int colWidth[] = new int[5];

	private final PropertyChangeListener securityChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(java.beans.PropertyChangeEvent evt) {
			Display display = dividendStatsListViewer.getTable().getDisplay();
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					dividendStatsListViewer.setInput(getDividendStats());
				}
			});
		}
	};

	private class DividendStatsListLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider {
		@Override
		public String getColumnText(Object element, int columnIndex) {
			DividendMonth divStats = (DividendMonth)element;
			switch (columnIndex) {
				case 0:
					return PeanutsUtil.formatDate(divStats.getMonth());
				case 1:
					return PeanutsUtil.formatCurrency(divStats.getAmountInDefaultCurrency(), Currencies.getInstance().getDefaultCurrency());
				case 2:
					return PeanutsUtil.formatCurrency(divStats.getYearlyAmount(), Currencies.getInstance().getDefaultCurrency());
				case 3:
					return PeanutsUtil.formatCurrency(divStats.getNettoInDefaultCurrency(), Currencies.getInstance().getDefaultCurrency());
				case 4:
					return PeanutsUtil.formatCurrency(divStats.getYearlyNetto(), Currencies.getInstance().getDefaultCurrency());

				default:
					break;
			}
			return null;
		}
		@Override
		public Color getForeground(Object element, int columnIndex) {
			return null;
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			return null;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
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
		Activator.getDefault().getAccountManager().getSecurities().stream()
			.forEach(s -> s.addPropertyChangeListener("dividends", securityChangeListener));
	}

	@Override
	public void dispose() {
		Activator.getDefault().getAccountManager().getSecurities().stream()
			.forEach(s -> s.removePropertyChangeListener("dividends", securityChangeListener));
		super.dispose();
	}

	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		TableColumn[] columns = dividendStatsListViewer.getTable().getColumns();
		for (int i = 0; i < columns.length; i++ ) {
			TableColumn tableColumn = columns[i];
			memento.putInteger("col" + i, tableColumn.getWidth());
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		dividendStatsListViewer = new TableViewer(parent, SWT.MULTI | SWT.FULL_SELECTION);
		Table table = dividendStatsListViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		ColumnViewerToolTipSupport.enableFor(dividendStatsListViewer);
		// must be called  before tableViewerColumn.setLabelProvider
		dividendStatsListViewer.setLabelProvider(new DividendStatsListLabelProvider());

		int colNum = 0;
		TableColumn col = new TableColumn(table, SWT.LEFT);
		col.setText("Month");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 300);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Amount");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 150);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Sum in year");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 150);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Netto");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 150);
		col.setResizable(true);
		colNum++;

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Netto sum in year");
		col.setWidth((colWidth[colNum] > 0) ? colWidth[colNum] : 150);
		col.setResizable(true);
		colNum++;

		dividendStatsListViewer.setContentProvider(new ArrayContentProvider());
		dividendStatsListViewer.setInput(getDividendStats());
	}

	private List<DividendMonth> getDividendStats() {
		return new DividendStats(Activator.getDefault().getAccountManager()).getDividendMonths();
	}

	@Override
	public void setFocus() {
	}

}
