package de.tomsplayground.peanuts.client.app;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

final class LoginDialog extends TitleAreaDialog {
	
	private Text filenameText;
	private Text passwordText;

	LoginDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite parentComposite = (Composite) super.createDialogArea(parent);

		Composite contents = new Composite(parentComposite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		contents.setLayout(layout);
		contents.setLayoutData(new GridData(GridData.FILL_BOTH));
		contents.setFont(parentComposite.getFont());

		Label label = new Label(contents, SWT.NONE);
		label.setText("File:");

		Composite fileChooserComposite = new Composite(contents, SWT.NONE);
		GridLayout gridLayout = new GridLayout(2, false);
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 0;
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		fileChooserComposite.setLayout(gridLayout);
		fileChooserComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		filenameText = new Text(fileChooserComposite, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		filenameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Button fileChooserButton = new Button(fileChooserComposite, SWT.FLAT);
		Image image = Activator.getDefault().getImageRegistry().get(Activator.IMAGE_LOAD_FILE);
		fileChooserButton.setImage(image);
		fileChooserButton.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fileDialog = new FileDialog(LoginDialog.this.getShell(), SWT.OPEN);
				fileDialog.setFilterExtensions(Activator.ALL_FILE_PATTERN);
				String selectedFilename = fileDialog.open();
				if (selectedFilename != null) {
					filenameText.setText(selectedFilename);
					passwordText.setEnabled(selectedFilename.endsWith("."+Activator.FILE_EXTENSION_SECURE));
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		label = new Label(contents, SWT.NONE);
		label.setText("Password:");

		passwordText = new Text(contents, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
		passwordText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		String filename = Activator.getDefault().getFilename();
		if (StringUtils.isNotBlank(filename)) {
			filenameText.setText(filename);
			passwordText.setEnabled(filename.endsWith("."+Activator.FILE_EXTENSION_SECURE));
			passwordText.setFocus();
		}

		return parentComposite;
	}
	
	@Override
	protected void okPressed() {
		String password = passwordText.getText();
		String filename = filenameText.getText();
		if (StringUtils.isBlank(filename)) {
			setErrorMessage("No input file selected.");
			return;
		}
		if (filename.endsWith("."+Activator.FILE_EXTENSION_SECURE) && StringUtils.isBlank(password)){
			setErrorMessage("Password must not be emtpy.");
			return;
		}
		
		Activator activator = Activator.getDefault();
		activator.setPassPhrase(password);
		try {
			activator.load(filename);
			super.okPressed();
		} catch (Exception e) {
			e.printStackTrace();
			setErrorMessage("Could not open file. Wrong password?");
		}
	}
}