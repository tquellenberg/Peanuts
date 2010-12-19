package de.tomsplayground.peanuts.client.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormColors;

public class FormToolkit extends org.eclipse.ui.forms.widgets.FormToolkit {

	public FormToolkit(Display display) {
		super(display);
	}

	public FormToolkit(FormColors colors) {
		super(colors);
	}

	@Override
	public Text createText(Composite parent, String value) {
		return super.createText(parent, value, SWT.BORDER);
	}
}
