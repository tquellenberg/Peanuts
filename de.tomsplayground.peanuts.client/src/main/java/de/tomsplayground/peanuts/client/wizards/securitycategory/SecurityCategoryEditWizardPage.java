package de.tomsplayground.peanuts.client.wizards.securitycategory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.Inventory;
import de.tomsplayground.peanuts.domain.base.InventoryEntry;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.statistics.SecurityCategoryMapping;

public class SecurityCategoryEditWizardPage extends WizardPage {

	private Text name;
	private final SecurityCategoryMapping mapping;
	private final String category;

	private final ModifyListener checkNotEmptyListener = new ModifyListener() {
		@Override
		public void modifyText(ModifyEvent e) {
			Text t = (Text)e.getSource();
			setPageComplete(StringUtils.isNotBlank(t.getText()));
		}
	};
	private List<Security> securities;
	private List<Security> selectedSecurities;
	private Button addButton;
	private Button removeButton;
	private ListViewer listViewer1;
	private ListViewer listViewer2;
	private Inventory inventory;
	private static final ViewerComparator SECURITY_SORTER = new ViewerComparator() {
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			return ((Security)e1).getName().compareToIgnoreCase(((Security)e2).getName());
		}
	};
	private ViewerFilter viewerFilter;

	protected SecurityCategoryEditWizardPage(String pageName, SecurityCategoryMapping mapping, String category) {
		super(pageName);
		this.mapping = mapping;
		this.category = category;
	}

	@Override
	public void createControl(Composite parent) {
		Composite contents = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout();
		contents.setLayout(layout);

		Label label = new Label(contents, SWT.NONE);
		label.setText("Name:");
		name = new Text(contents, SWT.SINGLE | SWT.BORDER);
		name.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		name.setText(category != null ? category : "");
		name.addModifyListener(checkNotEmptyListener);

		Composite securityChooser = new Composite(contents, SWT.NONE);
		GridLayout gridLayout = new GridLayout(3, false);
		gridLayout.marginWidth = 0;
		securityChooser.setLayout(gridLayout);

		listViewer1 = createList(securityChooser);

		// Buttons
		Composite buttonRow = new Composite(securityChooser, SWT.NONE);
		buttonRow.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		buttonRow.setLayout(new GridLayout());
		addButton = new Button(buttonRow, SWT.NONE);
		addButton.setText("Add -->");
		addButton.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));
		addButton.setEnabled(false);
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection = (IStructuredSelection) listViewer1.getSelection();
				@SuppressWarnings("unchecked")
				List<Security> all = selection.toList();
				securities.removeAll(all);
				selectedSecurities.addAll(all);
				listViewer1.refresh();
				listViewer2.refresh();
			}
		});

		removeButton = new Button(buttonRow, SWT.NONE);
		removeButton.setText("<-- Remove");
		removeButton.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));
		removeButton.setEnabled(false);
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection = (IStructuredSelection) listViewer2.getSelection();
				@SuppressWarnings("unchecked")
				List<Security> all = selection.toList();
				selectedSecurities.removeAll(all);
				securities.addAll(all);
				listViewer1.refresh();
				listViewer2.refresh();
			}
		});

		listViewer2 = createList(securityChooser);

		// Listener
		listViewer1.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				addButton.setEnabled(! event.getSelection().isEmpty());
			}
		});
		listViewer2.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				removeButton.setEnabled(! event.getSelection().isEmpty());
			}
		});

		// Content
		securities = new ArrayList<Security>(Activator.getDefault().getAccountManager().getSecurities());
		if (category != null) {
			selectedSecurities = new ArrayList<Security>(mapping.getSecuritiesByCategory(category));
		} else {
			selectedSecurities = new ArrayList<Security>();
		}
		securities.removeAll(selectedSecurities);
		listViewer1.setInput(securities);
		listViewer1.setComparator(SECURITY_SORTER);
		listViewer2.setInput(selectedSecurities);
		listViewer2.setComparator(SECURITY_SORTER);

		Composite buttonComposite = new Composite(securityChooser, SWT.NONE);
		GridLayout gridLayout2 = new GridLayout(2, false);
		gridLayout2.marginWidth = 0;
		gridLayout2.marginHeight = 0;
		buttonComposite.setLayout(gridLayout2);
		final Button filterButton = new Button(buttonComposite, SWT.CHECK);
		filterButton.setSelection(true);
		filterButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (filterButton.getSelection()) {
					listViewer1.addFilter(viewerFilter);
				} else {
					listViewer1.resetFilters();
				}
			}
		});
		inventory = Activator.getDefault().getAccountManager().getFullInventory();
		viewerFilter = new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				Security security = (Security)element;
				InventoryEntry entry = inventory.getEntry(security);
				return (entry != null && entry.getQuantity().intValue() > 0);
			}
		};
		listViewer1.addFilter(viewerFilter);
		Label label2 = new Label(buttonComposite, SWT.NONE);
		label2.setText("Show only my securities");

		setControl(contents);
	}

	private ListViewer createList(Composite securityChooser) {
		ListViewer listViewer = new ListViewer(securityChooser);
		GridData layoutData = new GridData(SWT.LEFT, SWT.TOP, false, false);
		layoutData.heightHint = 250;
		layoutData.widthHint = 300;
		listViewer.getList().setLayoutData(layoutData);
		listViewer.setContentProvider(new ArrayContentProvider());
		listViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				Security security = (Security) element;
				String text = security.getName();
				if (StringUtils.isNoneBlank(mapping.getCategory(security))) {
					text += " ("+mapping.getCategory(security)+")";
				}
				return text;
			}
		});
		return listViewer;
	}

	@Override
	public String getName() {
		return name.getText();
	}

	public Set<Security> getSecurities() {
		return new HashSet<Security>(selectedSecurities);
	}

}
