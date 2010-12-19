package de.tomsplayground.peanuts.client.editors.securitycategory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableEditor;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.wizards.securitycategory.SecurityCategoryEditWizard;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.base.InventoryEntry;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.statistics.SecurityCategoryMapping;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class DetailPart extends EditorPart implements IPersistableEditor {

	private class LabelProvider extends org.eclipse.jface.viewers.LabelProvider implements ITableLabelProvider {

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof String) {
				if (columnIndex == 0) {
					return (String) element;
				}
				if (columnIndex == 1) {
					return PeanutsUtil.formatCurrency(mapping.calculateCategoryValues(inventory).get(element), Currency.getInstance("EUR"));
				}
			} else if (element instanceof Security) {
				if (columnIndex == 0) {
					return ((Security)element).getName();
				}
				if (columnIndex == 1) {
					Collection<InventoryEntry> entries = inventory.getEntries();
					for (InventoryEntry inventoryEntry : entries) {
						if (inventoryEntry.getSecurity().equals(element)) {
							// FIXME: Reports und Inventories brauchen eine Währung
							return PeanutsUtil.formatCurrency(inventoryEntry.getMarketValue(), Currency.getInstance("EUR"));
						}
					}
				}
			}
			return null;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
	}

	public static class ContentProvider implements ITreeContentProvider {

		private SecurityCategoryMapping mapping;

		@Override
		public void dispose() {
			// nothing to do
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// nothing to do
		}

		@Override
		public Object[] getElements(Object inputElement) {
			mapping = (SecurityCategoryMapping)inputElement;
			return mapping.getCategories().toArray(new String[0]);
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof String) {
				String category = (String) parentElement;
				List<Security> securities = new ArrayList<Security>(mapping.getSecuritiesByCategory(category));
				Collections.sort(securities, new Comparator<Security>() {
					@Override
					public int compare(Security o1, Security o2) {
						return o1.getName().compareToIgnoreCase(o2.getName());
					}
				});
				return securities.toArray();
			}
			return null;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return (element instanceof String);
		}
	}

	private TreeViewer treeViewer;
	private int colWidth[] = new int[2];
	private Inventory inventory;
	private Button editButton;
	private Button newButton;
	private Button removeButton;
	private SecurityCategoryMapping mapping;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if ( !(input instanceof SecurityCategoryEditorInput)) {
			throw new PartInitException("Invalid Input: Must be SecurityCategoryEditorInput");
		}
		setSite(site);
		setInput(input);
		setPartName(input.getName());
	}

	@Override
	public void createPartControl(Composite parent) {
		inventory = Activator.getDefault().getAccountManager().getFullInventory();
		
		mapping = ((SecurityCategoryEditorInput) getEditorInput()).getSecurityCategoryMapping();

		Composite top = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		top.setLayout(layout);

		treeViewer = new TreeViewer(top);
		Tree tree = treeViewer.getTree();
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		treeViewer.setContentProvider(new ContentProvider());
		treeViewer.setLabelProvider(new LabelProvider());
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
				if (singleCategorySelected(selection)) {
					editButton.setEnabled(true);
					removeButton.setEnabled(true);
				} else {
					editButton.setEnabled(false);
					removeButton.setEnabled(false);
				}
			}
		});

		TreeColumn col = new TreeColumn(tree, SWT.LEFT);
		col.setText("Name");
		col.setResizable(true);
		col.setWidth((colWidth[0] > 0) ? colWidth[0] : 200);

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Value");
		col.setResizable(true);
		col.setWidth((colWidth[1] > 0) ? colWidth[1] : 100);
		
		Composite buttonList = new Composite(top, SWT.NONE);
		buttonList.setLayout(new RowLayout());

		editButton = new Button(buttonList, SWT.NONE);
		editButton.setText("Edit...");
		editButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
				if (singleCategorySelected(selection)) {
					String category = (String) selection.getFirstElement();
					WizardDialog dialog = new WizardDialog(getSite().getShell(), new SecurityCategoryEditWizard(mapping, category));
					if (dialog.open() == Window.OK) {
						treeViewer.refresh();
					}
				}
			}
		});
		editButton.setEnabled(false);

		newButton = new Button(buttonList, SWT.NONE);
		newButton.setText("New...");
		newButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				WizardDialog dialog = new WizardDialog(getSite().getShell(), new SecurityCategoryEditWizard(mapping, null));
				if (dialog.open() == Window.OK) {
					treeViewer.refresh();
				}
			}
		});
		
		removeButton = new Button(buttonList, SWT.NONE);
		removeButton.setText("Remove");
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
				if (singleCategorySelected(selection)) {
					String category = (String) selection.getFirstElement();
					mapping.removeCategory(category);
					treeViewer.refresh();
				}
			}
		});
		removeButton.setEnabled(false);

		treeViewer.setInput(mapping);
	}

	private boolean singleCategorySelected(ITreeSelection selection) {
		return (!selection.isEmpty()) && 
				(selection.getPaths().length == 1) && 
				(selection.getFirstElement() instanceof String);
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		// nothing to do
	}

	@Override
	public void doSaveAs() {
		// nothing to do
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void setFocus() {
		// nothing to do
	}

	@Override
	public void restoreState(IMemento memento) {
		for (int i = 0; i < colWidth.length; i++ ) {
			Integer width = memento.getInteger("col" + i);
			if (width != null) {
				colWidth[i] = width.intValue();
			}
		}
	}

	@Override
	public void saveState(IMemento memento) {
		TreeColumn[] columns = treeViewer.getTree().getColumns();
		for (int i = 0; i < columns.length; i++ ) {
			TreeColumn tableColumn = columns[i];
			memento.putInteger("col" + i, tableColumn.getWidth());
		}
	}

}
