package de.tomsplayground.peanuts.client.navigation;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.core.commands.IStateListener;
import org.eclipse.core.commands.State;
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
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.handlers.ExpandAllHandler;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.ViewPart;

import com.google.common.collect.Lists;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.comparison.Comparison;
import de.tomsplayground.peanuts.client.comparison.ComparisonEditor;
import de.tomsplayground.peanuts.client.comparison.ComparisonInput;
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
import de.tomsplayground.peanuts.client.util.UniqueAsyncExecution;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.IDeletable;
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
	private final TreeParent root = new TreeParent("");

	private final PropertyChangeListener elementChangedListener = new UniqueAsyncExecution() {
		@Override
		public void doit(PropertyChangeEvent evt, Display display) {
			viewer.update(evt.getSource(), null);
		}
		@Override
		public Display getDisplay() {
			return getSite().getShell().getDisplay();
		}
	};
	private final PropertyChangeListener allElementsChangedListener = new UniqueAsyncExecution() {
		@Override
		public void doit(PropertyChangeEvent evt, Display display) {
			viewer.refresh();
		}
		@Override
		public Display getDisplay() {
			return getSite().getShell().getDisplay();
		}
	};

	private final ViewerFilter deletedFilter = new ViewerFilter() {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof IDeletable) {
				return ! ((IDeletable)element).isDeleted();
			}
			return true;
		}
	};

	private final IWorkbenchAdapter workbenchAdapter = new IWorkbenchAdapter() {

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

	private FilteredTree filteredTree;

	class TreeObject {
		private final String name;
		private final Object baseObject;

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
		private final List<TreeObject> children;

		public TreeParent(String name) {
			super(name, null);
			children = new ArrayList<TreeObject>();
		}

		public void addChild(TreeObject child) {
			children.add(child);
			child.setParent(this);
			if (child.baseObject instanceof ObservableModelObject) {
				((ObservableModelObject) child.baseObject).addPropertyChangeListener("name", elementChangedListener);
				((ObservableModelObject) child.baseObject).addPropertyChangeListener("deleted", allElementsChangedListener);
			}
			if (child.baseObject instanceof Security) {
				((Security) child.baseObject).addPropertyChangeListener(elementChangedListener);
			}
		}

		public void removeChild(TreeObject child) {
			children.remove(child);
			child.setParent(null);
			if (child.baseObject instanceof ObservableModelObject) {
				((ObservableModelObject) child.baseObject).removePropertyChangeListener(elementChangedListener);
				((ObservableModelObject) child.baseObject).removePropertyChangeListener(allElementsChangedListener);
			}
			if (child.baseObject instanceof Security) {
				((Security) child.baseObject).removePropertyChangeListener(elementChangedListener);
			}
		}

		public void setChildren(List<TreeObject> newChildren) {
			for (TreeObject o : children) {
				o.setParent(null);
			}
			children.clear();
			for (TreeObject o : newChildren) {
				addChild(o);
			}
		}

		public TreeObject[] getChildren() {
			return children.toArray(new TreeObject[children.size()]);
		}

		public TreeObject getChild(String name) {
			for (TreeObject child : children) {
				if (child.getName().equals(name)) {
					return child;
				}
			}
			return null;
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
					if (treeObject.getBaseObject() != null) {
						result[i] = treeObject.getBaseObject();
					} else {
						result[i] = treeObject;
					}
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
		if (element instanceof TreeParent) {
			TreeParent parent = (TreeParent)element;
			TreeObject[] children = parent.getChildren();
			for (TreeObject treeObject : children) {
				destroyModel(treeObject);
				parent.removeChild(treeObject);
			}
		}
	}

	private void updateModel() {
		AccountManager accountManager = Activator.getDefault().getAccountManager();
		updateElements(root, "Accounts", accountManager.getAccounts());
		updateElements(root, "Securities", accountManager.getSecurities());
		updateElements(root, "Reports", accountManager.getReports());
		updateElements(root, "Forecasts", accountManager.getForecasts());
		updateElements(root, "Credits", accountManager.getCredits());
		updateElements(root, "Security Categories", accountManager.getSecurityCategoryMappings());
		updateElements(root, "Saved Transactions", accountManager.getSavedTransactions());
		updateElements(root, "Comparisons", Lists.newArrayList(new Comparison()));
	}

	private void updateElements(TreeParent parent, String groupName, List<? extends INamedElement> elements) {
		TreeParent group = (TreeParent) parent.getChild(groupName);
		if (group == null) {
			group = new TreeParent(groupName);
			parent.addChild(group);
		}
		List<TreeObject> newChildList = new ArrayList<NavigationView.TreeObject>();
		for (INamedElement element : elements) {
			TreeObject treeObject = group.getChild(element.getName());
			if (treeObject != null && treeObject.getBaseObject() == element) {
				newChildList.add(treeObject);
			} else {
				newChildList.add(new TreeObject(element.getName(), element));
			}
		}
		group.setChildren(newChildList);
	}

	@Override
	public void createPartControl(Composite parent) {
		PatternFilter filter = new PatternFilter() {

		};
		filteredTree = new FilteredTree(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER, filter, true, true);
		filteredTree.setQuickSelectionMode(true);
		viewer = filteredTree.getViewer();
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider());
		viewer.getTree().setFont(Activator.getDefault().getNormalFont());
		viewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				if (e1 instanceof TreeParent && e1 instanceof TreeParent) {
					return 0;
				}
				if (e1 instanceof INamedElement && e2 instanceof INamedElement) {
					return INamedElement.NAMED_ELEMENT_ORDER.compare((INamedElement)e1, (INamedElement)e2);
				}
				return super.compare(viewer, e1, e2);
			}
		});
		updateModel();
		viewer.setInput(root);
		propertyChangeListener = new UniqueAsyncExecution() {
			@Override
			public void doit(PropertyChangeEvent evt, Display display) {
				updateModel();
				viewer.refresh();
			}
			@Override
			public Display getDisplay() {
				return getSite().getShell().getDisplay();
			}
		};
		Activator.getDefault().getAccountManager().addPropertyChangeListener(propertyChangeListener);

		viewer.addFilter(deletedFilter);
		State deletedFilterState = ToggleDeletedFilterHandler.getDeletedFilterState();
		deletedFilterState.addListener(new IStateListener() {
			@Override
			public void handleStateChange(State state, Object oldValue) {
				if (ArrayUtils.contains(viewer.getFilters(), deletedFilter)) {
					viewer.removeFilter(deletedFilter);
				} else {
					viewer.addFilter(deletedFilter);
				}
			}
		});

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
				} else if (baseObject instanceof Comparison) {
					IEditorInput input = new ComparisonInput();
					try {
						getSite().getWorkbenchWindow().getActivePage().openEditor(input, ComparisonEditor.ID);
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
				updateModel();
				viewer.refresh();
			}
		});
		actionBars.setGlobalActionHandler(ActionFactory.PROPERTIES.getId(), new PropertyDialogAction(getSite(), viewer));

		IHandlerService handlerService = getSite().getService(IHandlerService.class);
		ExpandAllHandler expandHandler = new ExpandAllHandler(viewer);
		handlerService.activateHandler(ExpandAllHandler.COMMAND_ID, expandHandler);

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
		filteredTree.setFocus();
		filteredTree.getFilterControl().selectAll();
	}

}
