package de.tomsplayground.peanuts.client.app;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.actions.RetargetAction;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

import de.tomsplayground.peanuts.client.actions.LoadAction;
import de.tomsplayground.peanuts.client.quicken.QifImportAction;


/**
 * An action bar advisor is responsible for creating, adding, and disposing of the actions added to
 * a workbench window. Each window will be populated with new actions.
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

	// Actions - important to allocate these only in makeActions, and then use them
	// in the fill methods.  This ensures that the actions aren't recreated
	// when fillActionBars is called with FILL_PROXY.
	private IWorkbenchAction aboutAction;
	private Action qifImportAction;
	private RetargetAction refreshAction;
	private IWorkbenchAction closeAction;
	private IWorkbenchAction saveAction;
	private Action loadAction;
	private IWorkbenchAction quitAction;
	private IWorkbenchAction propertiesAction;
	private IWorkbenchAction preferenceAction;


	public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
		super(configurer);
	}

	@Override
	protected void makeActions(final IWorkbenchWindow window) {
		// Creates the actions and registers them.
		// Registering is needed to ensure that key bindings work.
		// The corresponding commands keybindings are defined in the plugin.xml file.
		// Registering also provides automatic disposal of the actions when
		// the window is closed.

		aboutAction = ActionFactory.ABOUT.create(window);
		register(aboutAction);

		closeAction = ActionFactory.CLOSE.create(window);
		register(closeAction);

		saveAction = ActionFactory.SAVE.create(window);
		register(saveAction);

		refreshAction = (RetargetAction) ActionFactory.REFRESH.create(window);
		window.getPartService().addPartListener(refreshAction);
		register(refreshAction);

		qifImportAction = new QifImportAction(window);
		qifImportAction.setText("QIF Import");
		register(qifImportAction);

//		saveAction = new SaveAction(window);
//		saveAction.setText("Save");
//		register(saveAction);

		loadAction = new LoadAction(window);
		loadAction.setText("Load");
		register(loadAction);
		
		propertiesAction = ActionFactory.PROPERTIES.create(window);
		register(propertiesAction);
		
		preferenceAction = ActionFactory.PREFERENCES.create(window);
		register(preferenceAction);

		quitAction = ActionFactory.QUIT.create(window);
		register(quitAction);
	}

	@Override
	protected void fillMenuBar(IMenuManager menuBar) {
		IWorkbenchWindow window = getActionBarConfigurer().getWindowConfigurer().getWindow();

		MenuManager fileMenu = new MenuManager("&File", IWorkbenchActionConstants.M_FILE);
		MenuManager windowMenu = new MenuManager("&Window", IWorkbenchActionConstants.M_WINDOW);
		MenuManager helpMenu = new MenuManager("&Help", IWorkbenchActionConstants.M_HELP);

		menuBar.add(fileMenu);
		menuBar.add(windowMenu);
		// Add a group marker indicating where action set menus will appear.
		menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		menuBar.add(helpMenu);

		// File
		fileMenu.add(loadAction);
		fileMenu.add(saveAction);
		fileMenu.add(new Separator());
		fileMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		fileMenu.add(closeAction);
		fileMenu.add(new Separator());

		String newId = ActionFactory.NEW.getId();
		MenuManager newMenu = new MenuManager("New", newId);
		newMenu.add(new Separator(newId));
		newMenu.add(ContributionItemFactory.NEW_WIZARD_SHORTLIST.create(window));
		newMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		fileMenu.add(newMenu);

		fileMenu.add(qifImportAction);
		fileMenu.add(refreshAction);
		fileMenu.add(closeAction);
		fileMenu.add(new Separator(IWorkbenchActionConstants.FILE_END));
		fileMenu.add(propertiesAction);
		fileMenu.add(new Separator());
		fileMenu.add(quitAction);

		// Window
		windowMenu.add(ContributionItemFactory.VIEWS_SHORTLIST.create(window));
		windowMenu.add(preferenceAction);

		// Help
		helpMenu.add(aboutAction);
	}

	@Override
	protected void fillCoolBar(ICoolBarManager coolBar) {
		IToolBarManager toolbar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
		coolBar.add(new ToolBarContributionItem(toolbar, IWorkbenchActionConstants.TOOLBAR_FILE));
		coolBar.add(new ToolBarContributionItem(toolbar, "main"));
	}
}
