package de.tomsplayground.peanuts.client.editors.securitycategory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.widgets.PersistentColumWidth;
import de.tomsplayground.peanuts.client.wizards.securitycategory.SecurityCategoryEditWizard;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.base.InventoryEntry;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.currenncy.Currencies;
import de.tomsplayground.peanuts.domain.statistics.SecurityCategoryMapping;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class DetailPart extends EditorPart {

	private class LabelProvider extends org.eclipse.jface.viewers.LabelProvider implements ITableLabelProvider {

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof String s) {
				if (columnIndex == 0) {
					return s;
				}
				if (columnIndex == 1) {
					return PeanutsUtil.formatCurrency(mapping.calculateCategoryValues(inventory).get(s), Currencies.getInstance().getDefaultCurrency());
				}
			} else if (element instanceof Security security) {
				if (columnIndex == 0) {
					return security.getName();
				}
				if (columnIndex == 1) {
					// FIXME: Reports und Inventories brauchen eine Währung
					return PeanutsUtil.formatCurrency(calc(security), Currencies.getInstance().getDefaultCurrency());
				}
			}
			return null;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
	}

	private BigDecimal calc(Security sec) {
		InventoryEntry inventoryEntry = inventory.getEntry(sec);
		if (inventoryEntry != null) {
			return inventoryEntry.getMarketValue();
		}
		return BigDecimal.ZERO;
	}

	public class ContentProvider implements ITreeContentProvider {

		private static final String WITHOUT_CATEGORY = "Without category";
		private SecurityCategoryMapping content;

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
			content = (SecurityCategoryMapping)inputElement;
			List<String> categories = content.getCategories();
			if (! content.getAllSecurities().containsAll(getSecuritiesInInventory())) {
				categories.add(WITHOUT_CATEGORY);
			}
			return categories.toArray(new String[0]);
		}

		private Collection<Security> getSecuritiesInInventory() {
			return inventory.getSecuritiesWithNoneZeroQuantity();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof String category) {
				List<Security> securities;
				if (category.equals(WITHOUT_CATEGORY)) {
					securities = new ArrayList<Security>(getSecuritiesInInventory());
					securities.removeAll(content.getAllSecurities());
				} else {
					securities = new ArrayList<Security>(content.getSecuritiesByCategory(category));
					securities.retainAll(getSecuritiesInInventory());
				}
				securities.sort((a, b) -> calc(b).compareTo(calc(a)));
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
	private Inventory inventory;
	private Button editButton;
	private Button newButton;
	private Button removeButton;
	private SecurityCategoryMapping mapping;
	private Button upButton;
	private Button downButton;

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
		inventory = Activator.getDefault().getAccountManager().getFullInventory(Activator.getDefault().getExchangeRates());

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
		tree.setFont(Activator.getDefault().getNormalFont());
		treeViewer.setContentProvider(new ContentProvider());
		treeViewer.setLabelProvider(new LabelProvider());
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
				if (singleCategorySelected(selection)) {
					editButton.setEnabled(true);
					removeButton.setEnabled(true);
					upButton.setEnabled(true);
					downButton.setEnabled(true);
				} else {
					editButton.setEnabled(false);
					removeButton.setEnabled(false);
					upButton.setEnabled(false);
					downButton.setEnabled(false);
				}
			}
		});

		TreeColumn col = new TreeColumn(tree, SWT.LEFT);
		col.setText("Name");
		col.setResizable(true);
		col.setWidth(200);

		col = new TreeColumn(tree, SWT.RIGHT);
		col.setText("Value");
		col.setResizable(true);
		col.setWidth(100);
		
		new PersistentColumWidth(tree, Activator.getDefault().getPreferenceStore(), 
				getClass().getCanonicalName()+"."+getEditorInput().getName());

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

		upButton = new Button(buttonList, SWT.NONE);
		upButton.setText("Up");
		upButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
				if (singleCategorySelected(selection)) {
					String category = (String) selection.getFirstElement();
					List<String> categories = mapping.getCategories();
					int indexOf = categories.indexOf(category);
					if (indexOf > 0) {
						Collections.swap(categories, indexOf, indexOf-1);
						mapping.setCategories(categories);
						treeViewer.refresh();
					}
				}
			}
		});
		upButton.setEnabled(false);

		downButton = new Button(buttonList, SWT.NONE);
		downButton.setText("Down");
		downButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
				if (singleCategorySelected(selection)) {
					String category = (String) selection.getFirstElement();
					List<String> categories = mapping.getCategories();
					int indexOf = categories.indexOf(category);
					if (indexOf < (categories.size()-1)) {
						Collections.swap(categories, indexOf, indexOf+1);
						mapping.setCategories(categories);
						treeViewer.refresh();
					}
				}
			}
		});
		downButton.setEnabled(false);

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
		treeViewer.getTree().setFocus();
	}
}
