package de.tomsplayground.peanuts.client.views;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
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
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ViewPart;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.dnd.CategoryTransferData;
import de.tomsplayground.peanuts.client.dnd.PeanutsTransfer;
import de.tomsplayground.peanuts.client.editors.report.ReportEditor;
import de.tomsplayground.peanuts.client.editors.report.ReportEditorInput;
import de.tomsplayground.peanuts.client.widgets.CategoryContentProvider;
import de.tomsplayground.peanuts.client.widgets.CategoryLabelProvider;
import de.tomsplayground.peanuts.domain.base.Category;
import de.tomsplayground.peanuts.domain.base.Category.Type;
import de.tomsplayground.peanuts.domain.query.CategoryQuery;
import de.tomsplayground.peanuts.domain.reporting.transaction.Report;

public class CategoryTreeView extends ViewPart {

	public static final String ID = "de.tomsplayground.peanuts.client.catgegoryTreeView";

	private TreeViewer viewer;

	private Action deleteAction;

	private PropertyChangeListener propertyChangeListener;

	private Action addAction;

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		viewer.setContentProvider(new CategoryContentProvider());
		viewer.setLabelProvider(new CategoryLabelProvider());
		viewer.setInput(Activator.getDefault().getAccountManager().getCategories());
		viewer.expandAll();
		propertyChangeListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				viewer.setInput(Activator.getDefault().getAccountManager().getCategories());
			}
		};
		Activator.getDefault().getAccountManager().addPropertyChangeListener("category", propertyChangeListener);

		viewer.setColumnProperties(new String[] { "name" });
		viewer.setCellEditors(new CellEditor[]{new TextCellEditor(viewer.getTree())});
		viewer.setCellModifier(new ICellModifier() {

			@Override
			public boolean canModify(Object element, String property) {
				return (element instanceof Category);
			}

			@Override
			public Object getValue(Object element, String property) {
				Category cat = (Category)element;
				return cat.getName();
			}

			@Override
			public void modify(Object element, String property, Object value) {
				Category cat = (Category)((TreeItem)element).getData();
				cat.setName((String)value);
				viewer.update(cat, null);
			}

		});

		// Drag-Source
		final Tree tree = viewer.getTree();
		Transfer[] types = new Transfer[] { PeanutsTransfer.INSTANCE };
		int operations = DND.DROP_COPY | DND.DROP_LINK;
		final DragSource source = new DragSource(tree, operations);
		source.setTransfer(types);
		final Category[] dragSourceItem = new Category[1];
		source.addDragListener(new DragSourceListener() {
			@Override
			public void dragStart(DragSourceEvent event) {
				TreeItem[] selection = tree.getSelection();
				if (selection.length > 0) {
					event.doit = true;
					dragSourceItem[0] = (Category) selection[0].getData();
				} else {
					event.doit = false;
				}
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				event.data = new CategoryTransferData(dragSourceItem[0]);
			}

			@Override
			public void dragFinished(DragSourceEvent event) {
				// nothing to do
			}
		});
		// Double click => Open Report
		viewer.addOpenListener(new IOpenListener() {
			@Override
			public void open(OpenEvent event) {
				Category cat = (Category) ((IStructuredSelection) event.getSelection()).getFirstElement();
				Report report = new Report(cat.getName());
				report.setAccounts(Activator.getDefault().getAccountManager().getAccounts());
				report.addQuery(new CategoryQuery(cat));
				ReportEditorInput input = new ReportEditorInput(report);
				IWorkbenchWindow activeWorkbenchWindow = getSite().getWorkbenchWindow();
				try {
					activeWorkbenchWindow.getActivePage().openEditor(input, ReportEditor.ID);
				} catch (PartInitException e) {
					MessageDialog.openError(activeWorkbenchWindow.getShell(), "Error",
						"Error opening editor:" + e.getMessage());
				}
			}
		});
		MenuManager menu = new MenuManager();
		menu.setRemoveAllWhenShown(true);
		menu.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});
		viewer.getTree().setMenu(menu.createContextMenu(viewer.getTree()));
		getSite().registerContextMenu(menu, viewer);
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (event.getSelection().isEmpty()) {
					deleteAction.setEnabled(false);
					addAction.setEnabled(false);
				} else {
					IStructuredSelection sel = (IStructuredSelection) event.getSelection();
					Object[] objects = sel.toArray();
					boolean okay = true;
					for (Object o : objects) {
						if (! (o instanceof Category))
							okay = false;
					}
					deleteAction.setEnabled(okay);
					addAction.setEnabled(objects.length == 1);
				}
			}
		});

		IActionBars actionBars = getViewSite().getActionBars();
		deleteAction = new Action("Delete") {
			@Override
			public void run() {
				IStructuredSelection sel = (IStructuredSelection) getSite().getWorkbenchWindow().getSelectionService().getSelection();
				Object[] objects = sel.toArray();
				for (Object o : objects) {
					if (o instanceof Category) {
						Category cat = (Category) o;
						try {
							Activator.getDefault().getAccountManager().removeCategory(cat);
						} catch (IllegalStateException e) {
							MessageDialog.openError(getSite().getWorkbenchWindow().getShell(), "Error",
								"Can't delete category:" + e.getMessage());
						}
					}
				}
			}
		};
		deleteAction.setEnabled(false);
		addAction = new Action("Add") {
			@Override
			public void run() {
				IStructuredSelection sel = (IStructuredSelection) getSite().getWorkbenchWindow().getSelectionService().getSelection();
				Object[] objects = sel.toArray();
				Type type;
				Category parentCategory = null;
				if (objects[0] instanceof Category) {
					parentCategory = (Category) objects[0];
					type = parentCategory.getType();
				} else {
					type = (Type) objects[0];
				}
				Category cat = new Category("New Category", type);
				cat.setParent(parentCategory);
				Activator.getDefault().getAccountManager().addCategory(cat);
				viewer.refresh(true);
			}
		};
		addAction.setEnabled(false);
		actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), deleteAction);
		getSite().setSelectionProvider(viewer);
	}

	protected void fillContextMenu(IMenuManager manager) {
		IActionBars actionBars = getViewSite().getActionBars();
		manager.add(actionBars.getGlobalActionHandler(ActionFactory.DELETE.getId()));
		manager.add(addAction);
		// Other plug-ins can contribute actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	@Override
	public void dispose() {
		Activator.getDefault().getAccountManager().removePropertyChangeListener("category", propertyChangeListener);
		super.dispose();
	}

	@Override
	public void setFocus() {
		viewer.getTree().setFocus();
	}

}
