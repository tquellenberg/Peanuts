package de.tomsplayground.peanuts.client.wizards.securitycategory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
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
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.statistics.SecurityCategoryMapping;

public class SecurityCategoryEditWizardPage extends WizardPage {

	private Text name;
	private final SecurityCategoryMapping mapping;
	private final String category;

	private ModifyListener checkNotEmptyListener = new ModifyListener() {
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
		securityChooser.setLayout(new GridLayout(3, false));
		
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
		if (category != null)
			selectedSecurities = new ArrayList<Security>(mapping.getSecuritiesByCategory(category));
		else
			selectedSecurities = new ArrayList<Security>();
		securities.removeAll(selectedSecurities);		
		listViewer1.setInput(securities);
		listViewer2.setInput(selectedSecurities);

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
				return security.getName();
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
