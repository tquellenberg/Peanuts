package de.tomsplayground.peanuts.client.wizards.security;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

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
