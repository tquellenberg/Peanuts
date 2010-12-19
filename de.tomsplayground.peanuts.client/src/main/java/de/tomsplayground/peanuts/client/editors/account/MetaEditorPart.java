package de.tomsplayground.peanuts.client.editors.account;

import java.util.Currency;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.part.EditorPart;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.Account;

public class MetaEditorPart extends EditorPart {

	private class EditorPartForm extends ManagedForm {
		public EditorPartForm(FormToolkit toolkit, ScrolledForm form) {
			super(toolkit, form);
		}

		@Override
		public void dirtyStateChanged() {
			getEditor().editorDirtyStateChanged();
		}
	}

	private FormToolkit toolkit;

	private Text accountName;
	private Combo currency;
	private Combo accountType;
	private AccountEditor editor;

	private IManagedForm managedForm;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if ( !(input instanceof AccountEditorInput)) {
			throw new PartInitException("Invalid Input: Must be AccountEditorInput");
		}
		setSite(site);
		setInput(input);
		setPartName(input.getName());
		toolkit = createToolkit(site.getWorkbenchWindow().getWorkbench().getDisplay());
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
		section.setText("Account");
		Composite sectionClient = toolkit.createComposite(section);
		sectionClient.setLayout(new GridLayout(2, false));

		toolkit.createLabel(sectionClient, "Name");
		accountName = toolkit.createText(sectionClient, "");
		accountName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		accountName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				sectionPart.markDirty();
			}});

		toolkit.createLabel(sectionClient, "Currency");
		currency = new Combo(sectionClient, SWT.READ_ONLY);
		currency.setItems(Activator.getDefault().getCurrencies());
		currency.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				sectionPart.markDirty();
			}});

		toolkit.createLabel(sectionClient, "Type");
		accountType = new Combo(sectionClient, SWT.READ_ONLY);
		for (Account.Type t : Account.Type.values())
			accountType.add(t.toString());
		accountType.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				sectionPart.markDirty();
			}});

		section.setClient(sectionClient);

		setValues();
		managedForm.refresh();
	}

	public void initialize(AccountEditor newEditor) {
		this.editor = newEditor;
	}

	public AccountEditor getEditor() {
		return editor;
	}

	protected void setValues() {
		Account account = ((AccountEditorInput)getEditorInput()).getAccount();
		accountName.setText(account.getName());
		currency.setText(account.getCurrency().toString());
		accountType.setText(account.getType().toString());
	}

	@Override
	public void setFocus() {
		accountName.setFocus();
	}

	protected FormToolkit createToolkit(Display display) {
		return new de.tomsplayground.peanuts.client.widgets.FormToolkit(display);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		Account account = ((AccountEditorInput)getEditorInput()).getAccount();
		account.setName(accountName.getText());
		String typeName = accountType.getItem(accountType.getSelectionIndex());
		account.setType(Account.Type.valueOf(typeName));
		String currencyName = currency.getItem(currency.getSelectionIndex());
		account.setCurrency(Currency.getInstance(currencyName));
		managedForm.commit(true);
	}

	@Override
	public boolean isDirty() {
		return managedForm.isDirty();
	}

	@Override
	public void doSaveAs() {
		// nothing to do
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

}
