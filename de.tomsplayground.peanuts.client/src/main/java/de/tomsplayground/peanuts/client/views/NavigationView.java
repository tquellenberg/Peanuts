package de.tomsplayground.peanuts.client.views;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.ViewPart;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.dnd.PeanutsTransfer;
import de.tomsplayground.peanuts.client.dnd.SecurityTransferData;
import de.tomsplayground.peanuts.client.editors.account.AccountEditor;
import de.tomsplayground.peanuts.client.editors.account.AccountEditorInput;
import de.tomsplayground.peanuts.client.editors.credit.CreditEditor;
import de.tomsplayground.peanuts.client.editors.credit.CreditEditorInput;
import de.tomsplayground.peanuts.client.editors.forecast.ForecastEditor;
import de.tomsplayground.peanuts.client.editors.forecast.ForecastEditorInput;
import de.tomsplayground.peanuts.client.editors.report.ReportEditor;
import de.tomsplayground.peanuts.client.editors.report.ReportEditorInput;
import de.tomsplayground.peanuts.client.editors.security.SecurityEditor;
import de.tomsplayground.peanuts.client.editors.security.SecurityEditorInput;
import de.tomsplayground.peanuts.client.editors.securitycategory.SecurityCategoryEditor;
import de.tomsplayground.peanuts.client.editors.securitycategory.SecurityCategoryEditorInput;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.INamedElement;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.beans.ObservableModelObject;
import de.tomsplayground.peanuts.domain.process.Credit;
import de.tomsplayground.peanuts.domain.reporting.forecast.Forecast;
import de.tomsplayground.peanuts.domain.reporting.transaction.Report;
import de.tomsplayground.peanuts.domain.statistics.SecurityCategoryMapping;

public class NavigationView extends ViewPart {
	public static final String ID = "de.tomsplayground.peanuts.client.navigationView";

	private TreeViewer viewer;
	private TreeParent root = new TreeParent("");


	private PropertyChangeListener nameChangesListener = new PropertyChangeListener() {
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			viewer.update(evt.getSource(), null);
		}
	};

	IWorkbenchAdapter workbenchAdapter = new IWorkbenchAdapter() {

		@Override
		public Object[] getChildren(Object o) {
			return ((TreeParent) o).getChildren();
		}

		@Override
		public ImageDescriptor getImageDescriptor(Object object) {
			return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER);
		}

		@Override
		public String getLabel(Object o) {
			return ((TreeObject) o).getName();
		}

		@Override
		public Object getParent(Object o) {
			return ((TreeObject) o).getParent();
		}
	};

	private PropertyChangeListener propertyChangeListener;
	
	class TreeObject {
		private String name;
		private Object baseObject;

		private TreeParent parent;

		public TreeObject(String name, Object baseObject) {
			this.name = name;
			this.baseObject = baseObject;
		}

		public String getName() {
			return name;
		}

		public void setParent(TreeParent parent) {
			this.parent = parent;
		}

		public TreeParent getParent() {
			return parent;
		}

		@Override
		public String toString() {
			return getName();
		}

		public Object getBaseObject() {
			return baseObject;
		}
	}

	class TreeParent extends TreeObject implements IAdaptable {
		private List<TreeObject> children;

		public TreeParent(String name) {
			super(name, null);
			children = new ArrayList<TreeObject>();
		}

		public void addChild(TreeObject child) {
			children.add(child);
			child.setParent(this);
		}

		public void removeChild(TreeObject child) {
			children.remove(child);
			child.setParent(null);
		}

		public TreeObject[] getChildren() {
			return children.toArray(new TreeObject[children.size()]);
		}

		public boolean hasChildren() {
			return children.size() > 0;
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
			if (adapter.isAssignableFrom(IWorkbenchAdapter.class)) {
				return workbenchAdapter;
			}
			return null;
		}

	}

	class ViewContentProvider implements ITreeContentProvider {

		@Override
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
			// nothing to do
		}

		@Override
		public void dispose() {
			// nothing to do
		}

		@Override
		public Object[] getElements(Object parent) {
			return getChildren(parent);
		}

		@Override
		public Object getParent(Object child) {
			if (child instanceof TreeObject) {
				return ((TreeObject) child).getParent();
			}
			return null;
		}

		@Override
		public Object[] getChildren(Object parent) {
			if (parent instanceof TreeParent) {
				TreeObject[] children = ((TreeParent) parent).getChildren();
				Object result[] = new Object[children.length];
				for (int i = 0; i < children.length; i++ ) {
					TreeObject treeObject = children[i];
					if (treeObject.getBaseObject() != null)
						result[i] = treeObject.getBaseObject();
					else
						result[i] = treeObject;
				}
				return result;
			}
			return new Object[0];
		}

		@Override
		public boolean hasChildren(Object parent) {
			if (parent instanceof TreeParent) {
				return ((TreeParent) parent).hasChildren();
			}
			return false;
		}
	}
	
	private void destroyModel(TreeObject element) {
		if (element.getBaseObject() != null && element.getBaseObject() instanceof ObservableModelObject) {
			((ObservableModelObject)element.getBaseObject()).removePropertyChangeListener(nameChangesListener);
		}
		if (element instanceof TreeParent) {
			TreeParent parent = (TreeParent)element;
			TreeObject[] children = parent.getChildren();
			for (TreeObject treeObject : children) {
				destroyModel(treeObject);
				parent.removeChild(treeObject);
			}
		}
	}

	private TreeObject createModel() {
		destroyModel(root);
		
		AccountManager accountManager = Activator.getDefault().getAccountManager();
		addElements(root, "Accounts", accountManager.getAccounts());
		addElements(root, "Securities", accountManager.getSecurities());
		addElements(root, "Reports", accountManager.getReports());
		addElements(root, "Forecasts", accountManager.getForecasts());
		addElements(root, "Credits", accountManager.getCredits());
		addElements(root, "Security Categories", accountManager.getSecurityCategoryMappings());
		addElements(root, "Saved Transactions", accountManager.getSavedTransactions());
		
		return root;
	}

	private void addElements(TreeParent parent, String groupName, List<? extends INamedElement> elements) {
		TreeParent group = new TreeParent(groupName);
		for (INamedElement element : elements) {
			group.addChild(new TreeObject(element.getName(), element));
			if (element instanceof ObservableModelObject) {
				((ObservableModelObject) element).addPropertyChangeListener("name", nameChangesListener);
			}
		}
		parent.addChild(group);		
	}
	
	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new WorkbenchLabelProvider());
		viewer.setInput(createModel());
		propertyChangeListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				viewer.setInput(createModel());
			}
		};
		Activator.getDefault().getAccountManager().addPropertyChangeListener(propertyChangeListener);
		viewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent event) {
				ITreeSelection selection = (ITreeSelection) event.getViewer().getSelection();
				Object baseObject = selection.getFirstElement();
				if (baseObject instanceof Account) {
					Account account = (Account) baseObject;
					IEditorInput input = new AccountEditorInput(account);
					try {
						getSite().getWorkbenchWindow().getActivePage().openEditor(input,
							AccountEditor.ID);
					} catch (PartInitException e) {
						e.printStackTrace();
					}
				} else if (baseObject instanceof Security) {
					Security security = (Security) baseObject;
					IEditorInput input = new SecurityEditorInput(security);
					try {
						getSite().getWorkbenchWindow().getActivePage().openEditor(input,
							SecurityEditor.ID);
					} catch (PartInitException e) {
						e.printStackTrace();
					}
				} else if (baseObject instanceof Report) {
					Report report = (Report) baseObject;
					IEditorInput input = new ReportEditorInput(report);
					try {
						getSite().getWorkbenchWindow().getActivePage().openEditor(input,
							ReportEditor.ID);
					} catch (PartInitException e) {
						e.printStackTrace();
					}
				} else if (baseObject instanceof Forecast) {
					Forecast forecast = (Forecast) baseObject;
					IEditorInput input = new ForecastEditorInput(forecast);
					try {
						getSite().getWorkbenchWindow().getActivePage().openEditor(input,
								ForecastEditor.ID);
					} catch (PartInitException e) {
						e.printStackTrace();
					}
				} else if (baseObject instanceof Credit) {
					Credit credit = (Credit) baseObject;
					IEditorInput input = new CreditEditorInput(credit);
					try {
						getSite().getWorkbenchWindow().getActivePage().openEditor(input,
								CreditEditor.ID);
					} catch (PartInitException e) {
						e.printStackTrace();
					}
				} else if (baseObject instanceof SecurityCategoryMapping) {
					SecurityCategoryMapping credit = (SecurityCategoryMapping) baseObject;
					IEditorInput input = new SecurityCategoryEditorInput(credit);
					try {
						getSite().getWorkbenchWindow().getActivePage().openEditor(input,
								SecurityCategoryEditor.ID);
					} catch (PartInitException e) {
						e.printStackTrace();
					}
				}
			}
		});
		viewer.expandAll();
		
		MenuManager menu = new MenuManager("#popupMenu", "popupMenu");
		menu.setRemoveAllWhenShown(true);
		menu.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});
		viewer.getTree().setMenu(menu.createContextMenu(viewer.getTree()));
		getSite().registerContextMenu(menu, viewer);
		
		// Drag-Source
		final Tree tree = viewer.getTree();
		Transfer[] types = new Transfer[] { PeanutsTransfer.INSTANCE };
		int operations = DND.DROP_COPY | DND.DROP_LINK;
		final DragSource source = new DragSource(tree, operations);
		source.setTransfer(types);
		final Security[] dragSourceItem = new Security[1];
		source.addDragListener(new DragSourceListener() {
			@Override
			public void dragStart(DragSourceEvent event) {
				TreeItem[] selection = tree.getSelection();
				if (selection.length > 0 && selection[0].getData() instanceof Security) {
					event.doit = true;					
					dragSourceItem[0] = (Security) selection[0].getData();
				} else {
					event.doit = false;
				}
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				event.data = new SecurityTransferData(dragSourceItem[0]);
				event.doit = true;
			}

			@Override
			public void dragFinished(DragSourceEvent event) {
				// nothing to do
			}
		});


		IActionBars actionBars = getViewSite().getActionBars();
		actionBars.setGlobalActionHandler(ActionFactory.REFRESH.getId(), new Action("Refresh") {
			@Override
			public void run() {
				viewer.setInput(createModel());
			}
		});
		actionBars.setGlobalActionHandler(ActionFactory.PROPERTIES.getId(), new PropertyDialogAction(getSite(), viewer));
		
		getSite().setSelectionProvider(viewer);
	}

	protected void fillContextMenu(IMenuManager manager) {
		IActionBars actionBars = getViewSite().getActionBars();
		manager.add(actionBars.getGlobalActionHandler(ActionFactory.REFRESH.getId()));
		// Other plug-ins can contribute actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(new Separator("group.properties"));
	}

	@Override
	public void dispose() {
		destroyModel(root);
		Activator.getDefault().getAccountManager().removePropertyChangeListener(propertyChangeListener);
		super.dispose();
	}
	
	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

}
