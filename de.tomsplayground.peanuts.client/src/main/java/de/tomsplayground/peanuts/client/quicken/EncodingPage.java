package de.tomsplayground.peanuts.client.quicken;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.SortedMap;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class EncodingPage extends WizardPage {

	private Combo encoding;

	protected EncodingPage(String pageName) {
		super(pageName);
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		Label label = new Label(composite, SWT.NONE);
		label.setText("Encoding");

		encoding = new Combo(composite, SWT.READ_ONLY);
		SortedMap<String, Charset> availableCharsets = Charset.availableCharsets();
		String encodingStr[] = new String[availableCharsets.size()];
		int i = 0;
		int selected = 0;
		for (Map.Entry<String, Charset> entry : availableCharsets.entrySet()) {
			String key = entry.getKey();
			if (key.equalsIgnoreCase("ISO-8859-1")) {
				selected = i;
			}
			encodingStr[i++ ] = key;
		}
		encoding.setItems(encodingStr);
		encoding.select(selected);

		setControl(composite);
	}

	public String getSelectedCharset() {
		return encoding.getItem(encoding.getSelectionIndex());
	}

}
