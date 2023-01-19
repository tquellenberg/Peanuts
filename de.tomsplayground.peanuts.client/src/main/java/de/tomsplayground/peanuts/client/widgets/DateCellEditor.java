package de.tomsplayground.peanuts.client.widgets;

import java.time.Month;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;

import de.tomsplayground.peanuts.util.Day;

public class DateCellEditor extends CellEditor {

	private DateTime dateTime;

	public DateCellEditor(Composite parent) {
		super(parent, SWT.NONE);
	}

	@Override
	protected Control createControl(Composite parent) {

		int style = getStyle() | SWT.DATE;

		dateTime = new DateTime(parent, style);

		dateTime.addKeyListener(new KeyAdapter() {
			// hook key pressed - see PR 14201
			@Override
			public void keyPressed(KeyEvent e) {
				keyReleaseOccured(e);
			}
		});

		dateTime.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
				applyEditorValueAndDeactivate();
			}

		});

		dateTime.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE || e.detail == SWT.TRAVERSE_RETURN) {
					e.doit = false;
				}
			}
		});

		dateTime.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				DateCellEditor.this.focusLost();
			}
		});
		return dateTime;
	}

	@Override
	protected Object doGetValue() {
		Day date = Day.of(dateTime.getYear(), Month.of(dateTime.getMonth()+1), dateTime.getDay());
		return date;
	}

	@Override
	protected void doSetFocus() {
		dateTime.setFocus();
	}

	@Override
	protected void doSetValue(Object value) {
		Day cal = (Day)value;
		dateTime.setYear(cal.year);
		dateTime.setMonth(cal.getMonth().getValue()-1);
		dateTime.setDay(cal.day);
	}

	/**
	 * Applies the currently selected value and deactivates the cell editor
	 */
	void applyEditorValueAndDeactivate() {
		Object newValue = doGetValue();
		markDirty();
		boolean isValid = isCorrect(newValue);
		setValueValid(isValid);
		fireApplyEditorValue();
		deactivate();
	}

	@Override
	protected void focusLost() {
		if (isActivated()) {
			applyEditorValueAndDeactivate();
		}
	}

	@Override
	protected void keyReleaseOccured(KeyEvent keyEvent) {
		if (keyEvent.character == '\u001b') { // Escape character
			fireCancelEditor();
		} else if (keyEvent.character == '\t') { // tab key
			applyEditorValueAndDeactivate();
		}
	}

}
