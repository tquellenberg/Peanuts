package de.tomsplayground.peanuts.client.editors.report;

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
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.process.ITransaction;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.domain.process.LabeledTransaction;
import de.tomsplayground.peanuts.domain.reporting.transaction.Report;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class TransactionListEditorPart extends EditorPart {

	private static class TransactionTableLabelProvider extends LabelProvider implements
		ITableLabelProvider, ITableColorProvider {

		private final Color red;
		private final Report report;

		public TransactionTableLabelProvider(Color red, Report report) {
			this.red = red;
			this.report = report;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			ITransaction trans = (ITransaction) element;
			switch (columnIndex) {
				case 0:
					return PeanutsUtil.formatDate(trans.getDay());
				case 1:
					String t;
					if (trans instanceof InvestmentTransaction) {
						InvestmentTransaction investTrans = (InvestmentTransaction) trans;
						t = investTrans.getSecurity().getName();
					} else {
						t = trans.getMemo();
						if (trans instanceof LabeledTransaction) {
							LabeledTransaction bankTrans = (LabeledTransaction) trans;
							if (StringUtils.isNotEmpty(bankTrans.getLabel())) {
								t = bankTrans.getLabel() + ": " + t;
							}
						}
					}
					return t;
				case 2:
					return PeanutsUtil.formatCurrency(trans.getAmount(), null);
				case 3:
					return PeanutsUtil.formatCurrency(report.getBalance(trans), null);
				default:
					return "";
			}
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			return null;
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			ITransaction trans = (ITransaction) element;
			if (columnIndex == 2 && trans.getAmount().signum() == -1) {
				return red;
			}
			return null;
		}
	}

	private final int colWidth[] = new int[4];
	private TableViewer tableViewer;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if ( !(input instanceof ReportEditorInput)) {
			throw new PartInitException("Invalid Input: Must be ReportEditorInput");
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
		col.setText("Datum");
		col.setWidth((colWidth[0] > 0) ? colWidth[0] : 100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.LEFT);
		col.setText("Beschreibung");
		col.setWidth((colWidth[1] > 0) ? colWidth[1] : 300);
		col.setResizable(true);

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Betrag");
		col.setWidth((colWidth[2] > 0) ? colWidth[2] : 100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.RIGHT);
		col.setText("Saldo");
		col.setWidth((colWidth[3] > 0) ? colWidth[3] : 100);
		col.setResizable(true);

		Report report = ((ReportEditorInput) getEditorInput()).getReport();

		Color red = Activator.getDefault().getColorProvider().get(Activator.RED);
		tableViewer.setLabelProvider(new TransactionTableLabelProvider(red, report));
		tableViewer.setContentProvider(new ArrayContentProvider());
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		tableViewer.setInput(report.getTransactions());
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
