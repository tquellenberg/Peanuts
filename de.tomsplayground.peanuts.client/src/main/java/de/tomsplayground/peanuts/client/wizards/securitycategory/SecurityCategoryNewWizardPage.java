package de.tomsplayground.peanuts.client.wizards.securitycategory;

import java.util.Arrays;
import java.util.List;

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

public class SecurityCategoryNewWizardPage extends WizardPage {

	private Text name;
	private Text categories;
	
	private final ModifyListener checkNotEmptyListener = new ModifyListener() {
		@Override
		public void modifyText(ModifyEvent e) {
			Text t = (Text)e.getSource();
			setPageComplete(StringUtils.isNotBlank(t.getText()));
		}
	};

	protected SecurityCategoryNewWizardPage(String pageName) {
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
		label.setText("Categories:");
		categories = new Text(contents, SWT.MULTI | SWT.BORDER);
		GridData ldata = new GridData(SWT.FILL, SWT.CENTER, true, false);
		ldata.heightHint = categories.getLineHeight() * 3;
		categories.setLayoutData(ldata);

		setControl(contents);
	}

	@Override
	public String getName() {
		return name.getText();
	}
	
	public List<String> getCategories() {
		String text = categories.getText();
		String[] categoryNames = StringUtils.stripAll(StringUtils.split(text, categories.getLineDelimiter()));
		return Arrays.asList(categoryNames);
	}
}
