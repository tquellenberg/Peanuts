package de.tomsplayground.peanuts.client.editors.account;

import java.math.BigDecimal;
import java.util.Currency;
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

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.editors.ITransactionProviderInput;
import de.tomsplayground.peanuts.domain.base.ITransactionProvider;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.domain.reporting.investment.PerformanceAnalyzer;
import de.tomsplayground.peanuts.domain.reporting.investment.PerformanceAnalyzer.Value;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class InvestmentPerformanceEditorPart extends EditorPart {

	private TableViewer tableViewer;

	private static class PerformanceTableLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider {

		private final Currency currency;
		private final Color red;

		public PerformanceTableLabelProvider(Color red, Currency currency) {
			this.currency = currency;
			this.red = red;
		}
		
		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof Value) {
				Value value = (Value) element;
				switch (columnIndex) {
				case 0:
					return String.valueOf(value.getYear());
				case 1:
					return PeanutsUtil.formatCurrency(value.getMarketValue1(), currency);
				case 2:
					return PeanutsUtil.formatCurrency(value.getMarketValue2(), currency);
				case 3:
					return PeanutsUtil.formatCurrency(value.getAdditions(), currency);
				case 4:
					return PeanutsUtil.formatCurrency(value.getLeavings(), currency);
				case 5:
					return PeanutsUtil.formatCurrency(value.getAdditions().add(value.getLeavings()), currency);
				case 6:
					return PeanutsUtil.formatCurrency(value.getGainings(), currency);
				case 7:
					return PeanutsUtil.formatCurrency(value.getInvestedAvg(), currency);
				case 8:
					return PeanutsUtil.formatPercent(value.getGainingPercent());
				case 9:
					return PeanutsUtil.formatPercent(value.getIRR());
				}
			} else if (element instanceof BigDecimal[]) {
				BigDecimal[] sum = (BigDecimal[]) element;
				if (columnIndex == 3) {
					return PeanutsUtil.formatCurrency(sum[0], currency);
				}
				if (columnIndex == 4) {
					return PeanutsUtil.formatCurrency(sum[1], currency);
				}
				if (columnIndex == 5) {
					return PeanutsUtil.formatCurrency(sum[2], currency);
				}
				if (columnIndex == 6) {
					return PeanutsUtil.formatCurrency(sum[3], currency);
				}
			}
			return null;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			return null;
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			if (element instanceof Value) {
				Value value = (Value) element;
				if (columnIndex == 5) {
					BigDecimal v = value.getAdditions().add(value.getLeavings());
					if (v.signum() == -1) {
						return red;
					}
				} else if (columnIndex == 6 || columnIndex == 8) {
					if (value.getGainings().signum() == -1) {
						return red;
					}
				} else if (columnIndex == 9) {
					if (value.getIRR().signum() == -1) {
						return red;
					}
				}
			} else if (element instanceof BigDecimal) {
				BigDecimal[] sum = (BigDecimal[]) element;
				if (columnIndex == 3) {
					if (sum[0].signum() == -1) {
						return red;
					}
				}
				if (columnIndex == 4) {
					if (sum[1].signum() == -1) {
						return red;
					}
				}
				if (columnIndex == 5) {
					if (sum[2].signum() == -1) {
						return red;
					}
				}
				if (columnIndex == 6) {
					if (sum[3].signum() == -1) {
						return red;
					}
				}
			}
			return null;
		}
	}
	
	class MyArrayContentProvider extends ArrayContentProvider {
		@Override
		@SuppressWarnings("unchecked")
		public Object[] getElements(Object inputElement) {
			List<Value> value = (List<Value>) inputElement;
			BigDecimal sum[] = new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
			Object[] array = new Object[value.size() + 1];
			int i = 0;
			for (Value v : value) {
				array[i++] = v;
				sum[0] = sum[0].add(v.getAdditions());
				sum[1] = sum[1].add(v.getLeavings());
				sum[2] = sum[2].add(v.getAdditions().add(v.getLeavings()));
				sum[3] = sum[3].add(v.getGainings());
			}
			array[i] = sum;
			return array;
		}
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if ( !(input instanceof ITransactionProviderInput)) {
			throw new PartInitException("Invalid Input: Must be ITransactionProviderInput");
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
		col.setWidth(50);
		col.setResizable(true);

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Value (1.1)");
		col.setWidth(100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Value (31.12)");
		col.setWidth(100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Additions");
		col.setWidth(100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Leavings");
		col.setWidth(100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Saldo");
		col.setWidth(100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Gain/Lost");
		col.setWidth(100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Invested avg");
		col.setWidth(100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Gain/Lost (%)");
		col.setWidth(100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Gain/Lost (%)");
		col.setWidth(100);
		col.setResizable(true);

		ITransactionProvider transactions = ((ITransactionProviderInput) getEditorInput()).getTransactionProvider();
		Color red = Activator.getDefault().getColorProvider().get(Activator.RED);
		tableViewer.setLabelProvider(new PerformanceTableLabelProvider(red, transactions.getCurrency()));
		tableViewer.setContentProvider(new MyArrayContentProvider());
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		PerformanceAnalyzer analizer = new PerformanceAnalyzer(transactions, PriceProviderFactory.getInstance());
		tableViewer.setInput(analizer.getValues());
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

	@Override
	public void setFocus() {
		tableViewer.getTable().setFocus();
	}

}
