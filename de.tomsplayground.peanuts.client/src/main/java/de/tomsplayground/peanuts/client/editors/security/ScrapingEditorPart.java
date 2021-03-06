package de.tomsplayground.peanuts.client.editors.security;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
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

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.scraping.Scraping;

public class ScrapingEditorPart extends EditorPart {

	private FormToolkit toolkit;
	private EditorPartForm managedForm;
	private Text scrapingUrl;
	private Text testResultText;
	private boolean dirty;
	private Text xpath;

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
		if (!(input instanceof SecurityEditorInput)) {
			throw new PartInitException("Invalid Input: Must be SecurityEditorInput");
		}
		setSite(site);
		setInput(input);
		setPartName(input.getName());
		toolkit = createToolkit(site.getWorkbenchWindow().getWorkbench().getDisplay());
	}

	@Override
	public void createPartControl(Composite parent) {
		final ScrolledForm form = toolkit.createScrolledForm(parent);
		form.setText("Scraping");
		form.getBody().setLayout(new TableWrapLayout());
		managedForm = new EditorPartForm(toolkit, form);
		final SectionPart sectionPart = new SectionPart(form.getBody(), toolkit, ExpandableComposite.TITLE_BAR);
		managedForm.addPart(sectionPart);
		Section section = sectionPart.getSection();
		section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		section.setText("Scraping");
		Composite sectionClient = toolkit.createComposite(section);
		sectionClient.setLayout(new GridLayout(2, false));

		Security security = ((SecurityEditorInput)getEditorInput()).getSecurity();

		toolkit.createLabel(sectionClient, "URL");
		scrapingUrl = toolkit.createText(sectionClient, security.getConfigurationValue(Scraping.SCRAPING_URL));
		scrapingUrl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		scrapingUrl.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				sectionPart.markDirty();
			}});

		toolkit.createLabel(sectionClient, "XPath ");
		xpath = toolkit.createText(sectionClient, security.getConfigurationValue(Scraping.SCRAPING_XPATH));
		xpath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		xpath.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				sectionPart.markDirty();
			}});

		Composite buttonComposite = toolkit.createComposite(sectionClient);
		buttonComposite.setLayout(new RowLayout());

		Button createButton = toolkit.createButton(buttonComposite, "Test", SWT.NONE);
		createButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					execute(true);
				} catch (Exception e1) {
					MessageDialog.openError(getSite().getShell(), "Error",
						e1.getClass().getCanonicalName()+"\n"+StringUtils.defaultString(e1.getMessage()));
				}
			}
		});
		createButton = toolkit.createButton(buttonComposite, "Scrap", SWT.NONE);
		createButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					execute(false);
				} catch (Exception e1) {
					MessageDialog.openError(getSite().getShell(), "Error", e1.getMessage());
				}
			}
		});

		section.setClient(sectionClient);

		final SectionPart sectionPart2 = new SectionPart(form.getBody(), toolkit, ExpandableComposite.TITLE_BAR);
		Section section2 = sectionPart2.getSection();
		section2.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		section2.setText("Scraping result");
		Composite sectionClient2 = toolkit.createComposite(section2);
		sectionClient2.setLayout(new GridLayout(1, false));
		testResultText = toolkit.createText(sectionClient2, "", SWT.MULTI | SWT.READ_ONLY);
		GridData layoutData = new GridData(SWT.FILL, SWT.CENTER, true, true);
		layoutData.heightHint = 200;
		testResultText.setLayoutData(layoutData);

		section2.setClient(sectionClient2);

		managedForm.refresh();
	}

	protected void execute(boolean test) {
		Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();
		Scraping scraping = new Scraping(security);
		Price price = scraping.execute();
		if (!test && price != null) {
			IPriceProvider priceProvider = PriceProviderFactory.getInstance().getPriceProvider(security);
			priceProvider.setPrice(price);
			dirty = true;
			firePropertyChange(IEditorPart.PROP_DIRTY);
		}
		testResultText.setText(scraping.getResult());
	}

	protected FormToolkit createToolkit(Display display) {
		return new de.tomsplayground.peanuts.client.widgets.FormToolkit(display);
	}

	@Override
	public void setFocus() {
		scrapingUrl.setFocus();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		Security security = ((SecurityEditorInput)getEditorInput()).getSecurity();
		security.putConfigurationValue(Scraping.SCRAPING_URL, scrapingUrl.getText());
		security.putConfigurationValue(Scraping.SCRAPING_XPATH, xpath.getText());
		dirty = false;
		managedForm.commit(true);
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isDirty() {
		return managedForm.isDirty() || dirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

}
