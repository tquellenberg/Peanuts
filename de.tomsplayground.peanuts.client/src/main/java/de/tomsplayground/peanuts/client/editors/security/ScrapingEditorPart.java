package de.tomsplayground.peanuts.client.editors.security;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
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
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyXmlSerializer;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPather;
import org.htmlcleaner.XPatherException;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.process.PriceBuilder;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.util.Day;

public class ScrapingEditorPart extends EditorPart {

	private FormToolkit toolkit;
	private EditorPartForm managedForm;
	private Text scrapingUrl;
	private final Map<String, Text> xpathMap = new HashMap<String, Text>();
	private Text testResultText;
	private boolean dirty;

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
		scrapingUrl = toolkit.createText(sectionClient, security.getConfigurationValue("scaping.url"));
		scrapingUrl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		scrapingUrl.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				sectionPart.markDirty();
			}});

		for (String value : new String[]{"open", "close", "high", "low", "date"}) {
			toolkit.createLabel(sectionClient, "XPath "+value);
			Text xpath = toolkit.createText(sectionClient, security.getConfigurationValue("scaping."+value));
			xpath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			xpath.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					sectionPart.markDirty();
				}});
			xpathMap.put(value, xpath);
		}

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

	protected void execute(boolean test) throws IOException, XPatherException, ParseException {
		HtmlCleaner htmlCleaner = new HtmlCleaner();
		TagNode tagNode = htmlCleaner.clean(new URL(scrapingUrl.getText()));
		
		PrettyXmlSerializer xmlSerializer = new PrettyXmlSerializer(htmlCleaner.getProperties());
		String string = xmlSerializer.getAsString(tagNode);
		
		StringBuilder resultStr = new StringBuilder();
		PriceBuilder priceBuilder = new PriceBuilder();
		for (String key : new String[]{"open", "close", "high", "low", "date"}) {
			String xpath = xpathMap.get(key).getText();
			if (StringUtils.isNotEmpty(xpath)) {
				XPather xPather = new XPather(xpath);
				Object[] result = xPather.evaluateAgainstNode(tagNode);
				for (Object object : result) {
					resultStr.append('>').append(object).append('\n');
				}		
				if (result.length > 0) {
					String value = result[0].toString().trim();
					int i = StringUtils.indexOfAnyBut(value, "0123456789,.");
					if (i != -1) {
						value = value.substring(0, i);
					}
					if (value.indexOf(',') != -1 && value.indexOf('.') == -1) {
						value = value.replace(',', '.');
					}
					resultStr.append(key).append(": ").append(value).append('\n');
					if (key.endsWith("date")) {
						SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
						priceBuilder.setDay(Day.fromDate((dateFormat.parse(value))));
					} else {
						BigDecimal p = new BigDecimal(value);
						if (key.equals("open")) priceBuilder.setOpen(p);
						if (key.equals("close")) priceBuilder.setClose(p);
						if (key.equals("high")) priceBuilder.setHigh(p);
						if (key.equals("low")) priceBuilder.setLow(p);
					}
				}
			}
		}
		Price price = (Price) priceBuilder.build();
		resultStr.append("Price: ").append(price).append('\n');
		resultStr.append('\n').append(string);

		if (!test) {
			Security security = ((SecurityEditorInput) getEditorInput()).getSecurity();
			IPriceProvider priceProvider = PriceProviderFactory.getInstance().getPriceProvider(security);
			priceProvider.setPrice(price);
			dirty = true;
			firePropertyChange(IEditorPart.PROP_DIRTY);
		}
		testResultText.setText(resultStr.toString());
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
		security.putConfigurationValue("scaping.url", scrapingUrl.getText());
		for (String key : xpathMap.keySet()) {
			String xpath = xpathMap.get(key).getText();
			security.putConfigurationValue("scaping."+key, xpath);
		}
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
