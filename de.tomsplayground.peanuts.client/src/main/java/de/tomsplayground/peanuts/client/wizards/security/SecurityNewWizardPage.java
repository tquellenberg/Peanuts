package de.tomsplayground.peanuts.client.wizards.security;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.progress.WorkbenchJob;

import de.tomsplayground.peanuts.app.yahoo.YahooSecurity;
import de.tomsplayground.peanuts.app.yahoo.YahooSecuritySearcher;

public class SecurityNewWizardPage extends WizardPage {

	private Text ticker;
	private Text isin;
	private Text name;
	private final ModifyListener checkNotEmptyListener = new ModifyListener() {
		@Override
		public void modifyText(ModifyEvent e) {
			Text t = (Text)e.getSource();
			setPageComplete(StringUtils.isNotBlank(t.getText()));
		}
	};
	private class RefreshJob extends WorkbenchJob {
		private String query;
		public RefreshJob() {
			super("Refresh Job");
			setSystem(true);
		}
		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			monitor.beginTask("Refreshing", IProgressMonitor.UNKNOWN);
			if (StringUtils.length(query) > 3) {
				List<YahooSecurity> result = new YahooSecuritySearcher().search(query);
				if (! monitor.isCanceled() && ! yahooTickerSearchResult.getTable().isDisposed()) {
					yahooTickerSearchResult.setInput(result);
				}
			}
			monitor.done();
			return Status.OK_STATUS;
		};
		public void setQuery(String query) {
			this.query = query;
		}
	}
	private final RefreshJob refreshJob = new RefreshJob();
	private TableViewer yahooTickerSearchResult;

	private static class TableLabelProvider extends LabelProvider implements ITableLabelProvider {
		@Override
		public String getColumnText(Object element, int columnIndex) {
			YahooSecurity security = (YahooSecurity) element;
			switch (columnIndex) {
				case 0: return security.getSymbol();
				case 1: return security.getName();
				case 2: return security.getExchange();
			}
			return null;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
	}

	protected SecurityNewWizardPage(String pageName) {
		super(pageName);
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite contents = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		contents.setLayout(layout);
		contents.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label label = new Label(contents, SWT.NONE);
		label.setText("Name:");
		name = new Text(contents, SWT.SINGLE | SWT.BORDER);
		name.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		name.addModifyListener(checkNotEmptyListener);

		label = new Label(contents, SWT.NONE);
		label.setText("ISIN:");
		isin = new Text(contents, SWT.SINGLE | SWT.BORDER);
		isin.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		isin.addModifyListener(checkNotEmptyListener);

		label = new Label(contents, SWT.NONE);
		label.setText("Ticker:");
		ticker = new Text(contents, SWT.SINGLE | SWT.BORDER);
		ticker.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		ticker.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				refreshJob.cancel();
				refreshJob.setQuery(((Text)e.getSource()).getText());
				refreshJob.schedule(500);
			}
		});

		label = new Label(contents, SWT.NONE);
		label.setText("Yahoo:");
		label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		yahooTickerSearchResult = new TableViewer(contents);
		Table table = yahooTickerSearchResult.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		TableColumn col = new TableColumn(table, SWT.NONE);
		col.setText("Ticker");
		col.setWidth(100);
		col.setResizable(true);

		col = new TableColumn(table, SWT.NONE);
		col.setText("Name");
		col.setWidth(300);
		col.setResizable(true);

		col = new TableColumn(table, SWT.NONE);
		col.setText("Exchange");
		col.setWidth(100);
		col.setResizable(true);

		yahooTickerSearchResult.setContentProvider(new ArrayContentProvider());
		yahooTickerSearchResult.setLabelProvider(new TableLabelProvider());

		yahooTickerSearchResult.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Object firstElement = ((IStructuredSelection)event.getSelection()).getFirstElement();
				if (firstElement instanceof YahooSecurity) {
					YahooSecurity securoty = (YahooSecurity) firstElement;
					ticker.setText(securoty.getSymbol());
				}
			}
		});

		setControl(contents);
	}

	public String getSecurityTicker() {
		return ticker.getText();
	}

	public String getSecurityName() {
		return name.getText();
	}

	public String getIsin() {
		return isin.getText();
	}
}
