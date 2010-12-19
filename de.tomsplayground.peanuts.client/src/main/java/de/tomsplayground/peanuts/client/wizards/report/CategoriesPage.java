package de.tomsplayground.peanuts.client.wizards.report;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.widgets.CategoryContentProvider;
import de.tomsplayground.peanuts.client.widgets.CategoryLabelProvider;
import de.tomsplayground.peanuts.domain.base.Category;
import de.tomsplayground.peanuts.domain.base.Category.Type;


public class CategoriesPage extends WizardPage {

	private TreeViewer listViewer;
	boolean allCategories;

	protected CategoriesPage(String pageName) {
		super(pageName);
		setTitle("Select categories");
		setMessage("Only transactions of the selected categories will be included in the report.");
		setDescription("Select categories");
		setPageComplete(false);
	}

	void checkPath(TreeItem item, boolean checked, boolean grayed) {
		if (item == null) {
			return;
		}
		if (grayed) {
			checked = true;
		} else {
			int index = 0;
			TreeItem[] items = item.getItems();
			while (index < items.length) {
				TreeItem child = items[index];
				if (child.getGrayed() || checked != child.getChecked()) {
					checked = grayed = true;
					break;
				}
				index++ ;
			}
		}
		item.setChecked(checked);
		item.setGrayed(grayed);
		checkPath(item.getParentItem(), checked, grayed);
	}

	void checkItems(TreeItem item, boolean checked) {
		item.setGrayed(false);
		item.setChecked(checked);
		TreeItem[] items = item.getItems();
		for (int i = 0; i < items.length; i++ ) {
			checkItems(items[i], checked);
		}
	}

	@Override
	public void createControl(Composite parent) {
		Composite top = new Composite(parent, SWT.NONE);
		top.setLayout(new GridLayout(2, false));

		listViewer = new TreeViewer(top, SWT.CHECK);
		final Tree tree = listViewer.getTree();
		tree.setLinesVisible(true);
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		listViewer.setLabelProvider(new CategoryLabelProvider());
		listViewer.setContentProvider(new CategoryContentProvider());
		listViewer.setInput(Activator.getDefault().getAccountManager().getCategories());
		tree.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (event.detail == SWT.CHECK) {
					TreeItem item = (TreeItem) event.item;
					boolean checked = item.getChecked();
					checkItems(item, checked);
					checkPath(item.getParentItem(), checked, false);
				}
				setPageComplete(anyCategoryChecked());
			}
		});
		// Check, when expanding a branch.
		// "Select all" does not reach the children (Bug?)
		tree.addListener(SWT.Expand, new Listener() {
			@Override
			public void handleEvent(Event event) {
				TreeItem item = (TreeItem) event.item;
				if (item.getChecked() && !item.getGrayed()) {
					for (TreeItem childitem : item.getItems()) {
						childitem.setChecked(true);
					}
				}
			}
		});

		Composite buttons = new Composite(top, SWT.NONE);
		buttons.setLayout(new GridLayout(1, false));
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		Button selectAll = new Button(buttons, SWT.PUSH);
		selectAll.setText("Select all");
		selectAll.setLayoutData(new GridData(SWT.FILL, SWT.CANCEL, true, false));
		selectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TreeItem[] items = tree.getItems();
				for (TreeItem tableItem : items) {
					checkItems(tableItem, true);
				}
				allCategories = true;
				tree.setEnabled(false);
				setPageComplete(true);
			}
		});

		Button deselectAll = new Button(buttons, SWT.PUSH);
		deselectAll.setText("Deselect all");
		deselectAll.setLayoutData(new GridData(SWT.FILL, SWT.CANCEL, true, false));
		deselectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TreeItem[] items = tree.getItems();
				for (TreeItem tableItem : items) {
					checkItems(tableItem, false);
				}
				allCategories = false;
				tree.setEnabled(true);
				setPageComplete(false);
			}
		});

		setControl(top);
		allCategories = false;
	}

	private boolean anyCategoryChecked() {
		Tree tree = listViewer.getTree();
		TreeItem[] items = tree.getItems();
		for (TreeItem treeItem : items) {
			if (treeItem.getChecked()) {
				return true;
			}
		}
		return false;
	}

	public List<Category> getCategories() {
		if (allCategories)
			return null;
		List<Category> result = new ArrayList<Category>();
		Tree tree = listViewer.getTree();
		TreeItem[] items = tree.getItems();
		traverseTree(result, items);
		return result;
	}

	private void traverseTree(List<Category> result, TreeItem[] items) {
		for (TreeItem treeItem : items) {
			if (treeItem.getChecked()) {
				if ( !treeItem.getGrayed()) {
					if (treeItem.getData() instanceof Type) {
						result.addAll(Activator.getDefault().getAccountManager().getCategories(
							(Type) treeItem.getData()));
					} else if (treeItem.getData() instanceof Category) {
						result.add((Category) treeItem.getData());
					}
				} else {
					traverseTree(result, treeItem.getItems());
				}
			}
		}
	}

}
