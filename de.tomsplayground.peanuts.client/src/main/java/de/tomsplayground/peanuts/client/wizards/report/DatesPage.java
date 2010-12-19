package de.tomsplayground.peanuts.client.wizards.report;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.tomsplayground.peanuts.client.widgets.DateComposite;
import de.tomsplayground.peanuts.domain.query.DateQuery;


public class DatesPage extends WizardPage {

	private Combo rangeType;
	private DateComposite fromDate;
	private DateComposite toDate;
	private Text reportName;

	protected DatesPage(String pageName) {
		super(pageName);
	}

	@Override
	public void createControl(Composite parent) {
		Composite top = new Composite(parent, SWT.NONE);
		top.setLayout(new GridLayout(2, false));

		Label label = new Label(top, SWT.NONE);
		label.setText("Report name:");
		reportName = new Text(top, SWT.BORDER);
		label = new Label(top, SWT.NONE);
		label.setText("Range:");
		rangeType = new Combo(top, SWT.READ_ONLY);
		rangeType.add("All");
		rangeType.add("This year");
		rangeType.add("Last 12 month");
		rangeType.add("Manual");
		rangeType.select(0);
		rangeType.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				boolean manual = rangeType.getText().equals("Manual");
				fromDate.setEnabled(manual);
				toDate.setEnabled(manual);
			}
		});

		label = new Label(top, SWT.NONE);
		label.setText("From:");
		fromDate = new DateComposite(top, SWT.NONE);
		fromDate.setEnabled(false);

		label = new Label(top, SWT.NONE);
		label.setText("To:");
		toDate = new DateComposite(top, SWT.NONE);
		toDate.setEnabled(false);

		setControl(top);
	}

	public DateQuery getDateQuery() {
		switch (rangeType.getSelectionIndex()) {
			case 0:
				return new DateQuery(DateQuery.TimeRange.ALL);
			case 1:
				return new DateQuery(DateQuery.TimeRange.THIS_YEAR);
			case 2:
				return new DateQuery(DateQuery.TimeRange.LAST_12_MONTH);
			default:
				return new DateQuery(fromDate.getDay(), toDate.getDay());
		}
	}

	public String getReportName() {
		return reportName.getText();
	}

}
