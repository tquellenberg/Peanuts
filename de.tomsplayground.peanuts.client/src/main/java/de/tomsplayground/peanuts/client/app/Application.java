package de.tomsplayground.peanuts.client.app;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * This class controls all aspects of the application's execution
 */
public class Application implements IApplication {

	@Override
	public Object start(IApplicationContext context) throws Exception {
		Display display = PlatformUI.createDisplay();
		try {
			int open;
			TitleAreaDialog titleAreaDialog = new TitleAreaDialog(null) {
				private Text passwordText;
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
					label.setText("Pasword:");

					passwordText = new Text(contents, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
					passwordText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
					
					return parentComposite;
				}
				
				@Override
				protected void okPressed() {
					String password = passwordText.getText();
					Activator.getDefault().setPassPhrase(password);
					Activator.getDefault().getAccountManager();
					super.okPressed();
				}
			};
			do {
				open = titleAreaDialog.open();
			} while (open != Window.OK);
			
			int returnCode = PlatformUI.createAndRunWorkbench(display,
				new ApplicationWorkbenchAdvisor());
			if (returnCode == PlatformUI.RETURN_RESTART) {
				return IApplication.EXIT_RESTART;
			}
			return IApplication.EXIT_OK;
		} finally {
			display.dispose();
		}
	}

	@Override
	public void stop() {
		// nothing to do
	}

}
