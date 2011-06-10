package de.tomsplayground.peanuts.client.editors.report;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.part.EditorPart;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.widgets.CategoryContentProvider;
import de.tomsplayground.peanuts.client.widgets.CategoryLabelProvider;
import de.tomsplayground.peanuts.client.widgets.DateComposite;
import de.tomsplayground.peanuts.client.wizards.report.AccountListLabelProvider;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.Category;
import de.tomsplayground.peanuts.domain.base.Category.Type;
import de.tomsplayground.peanuts.domain.query.CategoryQuery;
import de.tomsplayground.peanuts.domain.query.DateQuery;
import de.tomsplayground.peanuts.domain.query.IQuery;
import de.tomsplayground.peanuts.domain.reporting.forecast.Forecast;
import de.tomsplayground.peanuts.domain.reporting.transaction.Report;

public class MetaEditorPart extends EditorPart {

	private FormToolkit toolkit;
	private EditorPartForm managedForm;
	private Text reportName;
	private TableViewer accountListViewer;
	protected boolean allCategories;
	private TreeViewer categoryListViewer;
	private Combo rangeType;
	private DateComposite fromDate;
	private DateComposite toDate;
	private TableViewer forecastViewer;

	private class EditorPartForm extends ManagedForm {
		public EditorPartForm(FormToolkit toolkit, ScrolledForm form) {
			super(toolkit, form);
		}

		@Override
		public void dirtyStateChanged() {
			firePropertyChange(IEditorPart.PROP_DIRTY);
		}
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if ( !(input instanceof ReportEditorInput)) {
			throw new PartInitException("Invalid Input: Must be ReportEditorInput");
		}
		setSite(site);
		setInput(input);
		setPartName(input.getName());
		toolkit = createToolkit(site.getWorkbenchWindow().getWorkbench().getDisplay());
	}

	protected FormToolkit createToolkit(Display display) {
		return new de.tomsplayground.peanuts.client.widgets.FormToolkit(display);
	}

	@Override
	public void createPartControl(Composite parent) {
		final ScrolledForm form = toolkit.createScrolledForm(parent);
		form.setText("Meta Data");
		form.getBody().setLayout(new TableWrapLayout());
		managedForm = new EditorPartForm(toolkit, form);
		final SectionPart sectionPart = new SectionPart(form.getBody(), toolkit, ExpandableComposite.TITLE_BAR);
		managedForm.addPart(sectionPart);
		Section section = sectionPart.getSection();
		TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB);
		section.setLayoutData(td);
		section.setText("Report");
		Composite sectionClient = toolkit.createComposite(section);
		sectionClient.setLayout(new GridLayout(3, false));

		toolkit.createLabel(sectionClient, "Name");
		reportName = toolkit.createText(sectionClient, "");
		GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gridData.horizontalSpan = 2;
		reportName.setLayoutData(gridData);
		reportName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				sectionPart.markDirty();
			}});
		
		toolkit.createLabel(sectionClient, "Range");
		rangeType = new Combo(sectionClient, SWT.READ_ONLY);
		rangeType.add("All");
		rangeType.add("This year");
		rangeType.add("Last 12 month");
		rangeType.add("Manual");
		rangeType.select(0);
		rangeType.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				boolean manual = rangeType.getText().equals("Manual");
				fromDate.setEnabled(manual);
				toDate.setEnabled(manual);
				sectionPart.markDirty();
			}
		});
		gridData = new GridData(SWT.LEFT, SWT.CENTER, true, false);
		gridData.horizontalSpan = 2;
		rangeType.setLayoutData(gridData);

		toolkit.createLabel(sectionClient, "From");
		fromDate = new DateComposite(sectionClient, SWT.NONE);
		fromDate.setEnabled(false);
		gridData = new GridData(SWT.LEFT, SWT.CENTER, true, false);
		gridData.horizontalSpan = 2;
		fromDate.setLayoutData(gridData);
		
		toolkit.createLabel(sectionClient, "To");
		toDate = new DateComposite(sectionClient, SWT.NONE);
		toDate.setEnabled(false);
		gridData = new GridData(SWT.LEFT, SWT.CENTER, true, false);
		gridData.horizontalSpan = 2;
		toDate.setLayoutData(gridData);

		Label l = toolkit.createLabel(sectionClient, "Config");
		l.setLayoutData(new GridData(SWT.TOP, SWT.LEFT, false, false));
		Group top = new Group(sectionClient, SWT.NONE);
		top.setText("Accounts");
		top.setLayout(new GridLayout(2, false));

		accountListViewer = new TableViewer(top, SWT.CHECK);
		accountListViewer.getTable().setLinesVisible(true);
		accountListViewer.getTable().setLayoutData(new GridData(SWT.TOP, SWT.FILL, true, true));
		accountListViewer.setLabelProvider(new AccountListLabelProvider());
		accountListViewer.setContentProvider(new ArrayContentProvider());
		ImmutableList<Account> accounts = Activator.getDefault().getAccountManager().getAccounts();
		accountListViewer.setInput(accounts);
		accountListViewer.getTable().addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (event.detail == SWT.CHECK) {
					sectionPart.markDirty();
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
				// FIXME: sollte ähnlich wie bei Categories sein
				TableItem[] items = accountListViewer.getTable().getItems();
				for (TableItem tableItem : items) {
					tableItem.setChecked(true);
				}
				sectionPart.markDirty();
			}
		});

		Button deselectAll = new Button(buttons, SWT.PUSH);
		deselectAll.setText("Deselect all");
		deselectAll.setLayoutData(new GridData(SWT.FILL, SWT.CANCEL, true, false));
		deselectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TableItem[] items = accountListViewer.getTable().getItems();
				for (TableItem tableItem : items) {
					tableItem.setChecked(false);
				}
				sectionPart.markDirty();
			}
		});

		
		top = new Group(sectionClient, SWT.NONE);
		top.setText("Categories");
		top.setLayout(new GridLayout(2, false));
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		categoryListViewer = new TreeViewer(top, SWT.CHECK);
		
		final Tree tree = categoryListViewer.getTree();
		tree.setLinesVisible(true);
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		categoryListViewer.setLabelProvider(new CategoryLabelProvider());
		categoryListViewer.setContentProvider(new CategoryContentProvider());
		categoryListViewer.setInput(Activator.getDefault().getAccountManager().getCategories());
		categoryListViewer.expandAll();
		tree.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (event.detail == SWT.CHECK) {
					TreeItem item = (TreeItem) event.item;
					boolean checked = item.getChecked();
					checkItems(new TreeItem[]{item}, checked);
					checkPath(item.getParentItem(), checked, false);
					sectionPart.markDirty();
				}
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

		buttons = new Composite(top, SWT.NONE);
		buttons.setLayout(new GridLayout(1, false));
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		selectAll = new Button(buttons, SWT.PUSH);
		selectAll.setText("Select all");
		selectAll.setLayoutData(new GridData(SWT.FILL, SWT.CANCEL, true, false));
		selectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkItems(tree.getItems(), true);
				allCategories = true;
				sectionPart.markDirty();
			}
		});

		deselectAll = new Button(buttons, SWT.PUSH);
		deselectAll.setText("Deselect all");
		deselectAll.setLayoutData(new GridData(SWT.FILL, SWT.CANCEL, true, false));
		deselectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkItems(tree.getItems(), false);
				allCategories = false;
				sectionPart.markDirty();
			}
		});

		l = toolkit.createLabel(sectionClient, "Forecasts");
		l.setLayoutData(new GridData(SWT.TOP, SWT.LEFT, false, false));
		top = new Group(sectionClient, SWT.NONE);
		top.setLayout(new GridLayout(1, false));
		forecastViewer = new TableViewer(top, SWT.CHECK);
		forecastViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((Forecast)element).getName();
			}
		});
		forecastViewer.setContentProvider(new ArrayContentProvider());
		forecastViewer.setInput(Activator.getDefault().getAccountManager().getForecasts());
		forecastViewer.getTable().addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (event.detail == SWT.CHECK) {
					sectionPart.markDirty();
				}
			}
		});
		
		section.setClient(sectionClient);

		setValues();
		managedForm.refresh();
	}
	
	private void checkPath(TreeItem item, boolean checked, boolean grayed) {
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

	private void checkItems(TreeItem[] items, boolean checked) {
		for (TreeItem item : items) {
			item.setGrayed(false);
			item.setChecked(checked);
			checkItems(item.getItems(), checked);
		}
	}

	private void setValues() {
		Report report = ((ReportEditorInput)getEditorInput()).getReport();
		reportName.setText(report.getName());		
		TableItem[] items = accountListViewer.getTable().getItems();
		Set<Account> accounts = report.getAccounts();
		for (TableItem tableItem : items) {
			Account a = (Account) tableItem.getData();
			tableItem.setChecked(accounts.contains(a) || report.allAccounts());
		}
		items = forecastViewer.getTable().getItems();
		for (TableItem tableItem : items) {
			Forecast f = (Forecast) tableItem.getData();
			tableItem.setChecked(f.isConnected(report));
		}
		TreeItem[] items2 = categoryListViewer.getTree().getItems();
		Set<IQuery> queries = report.getQueries();
		boolean hasCategoryQuery = false;
		for (IQuery query : queries) {
			if (query instanceof CategoryQuery) {
				CategoryQuery cat = (CategoryQuery) query;
				Set<Category> categories = cat.getCategories();
				checkTreeItems(items2, categories);
				hasCategoryQuery = true;
			} else if (query instanceof DateQuery) {
				DateQuery dateQuery = (DateQuery) query;
				setDateQuery(dateQuery);
			}
		}
		if (! hasCategoryQuery) {
			allCategories = true;
			checkItems(items2, true);
		}
	}
	
	public List<Category> getCategories() {
		if (allCategories)
			return null;
		List<Category> result = new ArrayList<Category>();
		Tree tree = categoryListViewer.getTree();
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

	private void checkTreeItems(TreeItem[] items2, Set<Category> categories) {
		for (TreeItem treeItem : items2) {
			if (treeItem.getData() instanceof Category) {
				Category cat = (Category) treeItem.getData();
				boolean checked = categories.contains(cat);
				if (checked) {
					checkItems(new TreeItem[]{treeItem}, true);
					checkPath(treeItem, true, false);
				}
			}
			checkTreeItems(treeItem.getItems(), categories);				
		}
	}
	
	public Set<Account> getAccounts() {
		Set<Account> accounts = new HashSet<Account>();
		TableItem[] items = accountListViewer.getTable().getItems();
		for (TableItem tableItem : items) {
			if (tableItem.getChecked()) {
				accounts.add((Account) tableItem.getData());
			}
		}
		return accounts;
	}

	private void saveForecasts() {
		Report report = ((ReportEditorInput)getEditorInput()).getReport();
		TableItem[] items = forecastViewer.getTable().getItems();
		for (TableItem tableItem : items) {
			Forecast forecast = (Forecast) tableItem.getData();
			if (tableItem.getChecked()) {
				forecast.connect(report);
			} else {
				forecast.disconnect(report);
			}
		}
	}

	@Override
	public void setFocus() {
		reportName.setFocus();
	}

	private DateQuery getDateQuery() {
		switch (rangeType.getSelectionIndex()) {
			case 0:
				return new DateQuery(DateQuery.TimeRange.ALL);
			case 1:
				return new DateQuery(DateQuery.TimeRange.THIS_YEAR);
			case 2:
				return new DateQuery(DateQuery.TimeRange.LAST_12_MONTH);
			default:
				return new DateQuery(fromDate.getDay(), toDate.getDay());
		}
	}
	
	private void setDateQuery(DateQuery query) {
		switch (query.getTimeRange()) {
		case ALL:
			rangeType.select(0);
			break;
		case THIS_YEAR:
			rangeType.select(1);
			break;
		case LAST_12_MONTH:
			rangeType.select(2);
			break;
		case MANUAL:
			rangeType.select(3);
			fromDate.setDay(query.getStart());
			toDate.setDay(query.getEnd());
			break;
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		Report report = ((ReportEditorInput)getEditorInput()).getReport();
		report.setName(reportName.getText());
		report.setAccounts(getAccounts());		
		report.clearQueries();
		List<Category> categories = getCategories();
		if (categories != null)
			report.addQuery(new CategoryQuery(categories));
		report.addQuery(getDateQuery());
		saveForecasts();
		managedForm.commit(true);
	}

	@Override
	public boolean isDirty() {
		return managedForm.isDirty();
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void doSaveAs() {
		// nothing to do
	}

}
