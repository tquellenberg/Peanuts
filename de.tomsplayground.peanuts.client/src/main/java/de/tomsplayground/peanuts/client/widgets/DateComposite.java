package de.tomsplayground.peanuts.client.widgets;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.util.Day;


public class DateComposite extends Composite {

	private Button dateButton;
	private DateTime date;
	List<ModifyListener> modifyListener = new ArrayList<ModifyListener>();

	public DateComposite(Composite parent, int style) {
		super(parent, style);
		GridLayout gridLayout = new GridLayout(2, false);
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 0;
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		setLayout(gridLayout);

		dateButton = new Button(this, SWT.FLAT);
		Image image = Activator.getDefault().getImageRegistry().get(Activator.IMAGE_CALENDAR);
		dateButton.setImage(image);
		date = new DateTime(this, SWT.DATE | SWT.READ_ONLY);

		dateButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final Shell dialog = new Shell(getShell(), SWT.DIALOG_TRIM);
				dialog.setLayout(new GridLayout(1, false));
				final DateTime calendar = new DateTime(dialog, SWT.CALENDAR | SWT.BORDER);
				calendar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				calendar.setYear(date.getYear());
				calendar.setMonth(date.getMonth());
				calendar.setDay(date.getDay());
				Button ok = new Button(dialog, SWT.PUSH);
				ok.setText("OK");
				ok.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
				ok.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e2) {
						date.setDay(calendar.getDay());
						date.setMonth(calendar.getMonth());
						date.setYear(calendar.getYear());
						for (ModifyListener listener : modifyListener) {
							Event event = new Event();
							event.widget = DateComposite.this;
							listener.modifyText(new ModifyEvent(event));
						}
						dialog.close();
					}
				});
				dialog.setDefaultButton(ok);
				dialog.pack();
				Point size = dialog.getSize();
				size.x = size.x + 10;
				size.y = size.y + 10;
				dialog.setSize(size);
				Monitor primary = getDisplay().getPrimaryMonitor();
				Rectangle bounds = primary.getBounds();
				Rectangle rect = dialog.getBounds();
				int x = bounds.x + (bounds.width - rect.width) / 2;
				int y = bounds.y + (bounds.height - rect.height) / 2;
				dialog.setLocation(x, y);
				dialog.open();
			}
		});
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		dateButton.setEnabled(enabled);
		date.setEnabled(enabled);
	}

	public Calendar getDate() {
		Calendar newDate = Calendar.getInstance();
		newDate.clear();
		newDate.set(date.getYear(), date.getMonth(), date.getDay());
		return newDate;
	}
	
	public Day getDay() {
		return new Day(date.getYear(), date.getMonth(), date.getDay());
	}

	public void setDay(Day day) {
		date.setYear(day.getYear());
		date.setMonth(day.getMonth());
		date.setDay(day.getDay());
	}

	public void setDate(Calendar calendar) {
		date.setYear(calendar.get(Calendar.YEAR));
		date.setMonth(calendar.get(Calendar.MONTH));
		date.setDay(calendar.get(Calendar.DAY_OF_MONTH));
	}

	public void addModifyListener(final ModifyListener listener) {
		modifyListener.add(listener);
		date.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Event event = new Event();
				event.widget = DateComposite.this;
				listener.modifyText(new ModifyEvent(event));
			}
		});
	}

}
