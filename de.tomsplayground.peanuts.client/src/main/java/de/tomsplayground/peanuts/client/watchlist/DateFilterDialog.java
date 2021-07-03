package de.tomsplayground.peanuts.client.watchlist;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import de.tomsplayground.peanuts.client.widgets.DateComposite;
import de.tomsplayground.peanuts.util.Day;

public class DateFilterDialog extends Dialog {

	private DateComposite datePickerFrom;
	private DateComposite datePickerTo;

	private Day startDay;
	private Day endDay;

	public DateFilterDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Custom performance range");
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		((GridLayout) composite.getLayout()).numColumns = 2;
		((GridLayout) composite.getLayout()).makeColumnsEqualWidth = false;

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.widthHint = convertHorizontalDLUsToPixels(150);
		composite.setLayoutData(gd);

		Label label = new Label(composite, SWT.NONE);
		label.setText("From:");
		label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		datePickerFrom = new DateComposite(composite, SWT.NONE);
		datePickerFrom.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if (startDay == null) {
			startDay = Day.today();
		}
		datePickerFrom.setDay(startDay);
		datePickerFrom.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				startDay = datePickerFrom.getDay();
			}
		});

		label = new Label(composite, SWT.NONE);
		label.setText("To:");
		label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		datePickerTo = new DateComposite(composite, SWT.NONE);
		datePickerTo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if (endDay == null) {
			endDay = Day.today();
		}
		datePickerTo.setDay(endDay);
		datePickerTo.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				endDay = datePickerTo.getDay();
			}
		});

		return composite;
	}

	public Day getStartDay() {
		return startDay;
	}

	public void setStartDay(Day startDay) {
		this.startDay = startDay;
	}

	public Day getEndDay() {
		return endDay;
	}

	public void setEndDay(Day endDay) {
		this.endDay = endDay;
	}

}