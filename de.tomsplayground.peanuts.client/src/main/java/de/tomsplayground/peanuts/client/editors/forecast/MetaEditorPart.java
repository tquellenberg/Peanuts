package de.tomsplayground.peanuts.client.editors.forecast;

import java.math.BigDecimal;
import java.text.ParseException;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
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

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.widgets.CalculatorText;
import de.tomsplayground.peanuts.client.widgets.DateComposite;
import de.tomsplayground.peanuts.domain.reporting.forecast.Forecast;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class MetaEditorPart extends EditorPart {

	private FormToolkit toolkit;
	private EditorPartForm managedForm;
	private Text forecastName;
	private DateComposite startDate;
	private Text startAmount;
	private Text annualIncrease;
	private Text annualIncreasePercent;
	private Text resultText;

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
		if ( !(input instanceof ForecastEditorInput)) {
			throw new PartInitException("Invalid Input: Must be ForecastEditorInput");
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
		section.setText("Forecast");
		Composite top = toolkit.createComposite(section);
		top.setLayout(new GridLayout(2, false));

		Label label = new Label(top, SWT.NONE);
		label.setText("Forecast name:");
		forecastName = new Text(top, SWT.BORDER);
		forecastName.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		forecastName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				sectionPart.markDirty();
			}
		});

		label = new Label(top, SWT.NONE);
		label.setText("From:");
		startDate = new DateComposite(top, SWT.NONE);
		startDate.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				sectionPart.markDirty();
			}
		});

		label = new Label(top, SWT.NONE);
		label.setText("Start amount:");
		startAmount = (new CalculatorText(top, SWT.SINGLE | SWT.BORDER)).getText();
		startAmount.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		startAmount.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				sectionPart.markDirty();
			}
		});

		label = new Label(top, SWT.NONE);
		label.setText("Annual increase (absolut):");
		annualIncrease = (new CalculatorText(top, SWT.SINGLE | SWT.BORDER)).getText();
		annualIncrease.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		annualIncrease.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				sectionPart.markDirty();
			}
		});

		label = new Label(top, SWT.NONE);
		label.setText("Annual increase (percent):");
		annualIncreasePercent = (new CalculatorText(top, SWT.SINGLE | SWT.BORDER)).getText();
		annualIncreasePercent.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		annualIncreasePercent.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				sectionPart.markDirty();
			}
		});

		section.setClient(top);

		setValues();

		final SectionPart sectionPart2 = new SectionPart(form.getBody(), toolkit, ExpandableComposite.TITLE_BAR);
		managedForm.addPart(sectionPart2);
		Section section2 = sectionPart2.getSection();
		td = new TableWrapData(TableWrapData.FILL_GRAB);
		section2.setLayoutData(td);
		section2.setText("Calculation");
		Composite top2 = toolkit.createComposite(section2);
		top2.setLayout(new GridLayout(2, false));

		label = new Label(top2, SWT.NONE);
		label.setText("Date:");
		final DateComposite calcDate = new DateComposite(top2, SWT.NONE);
		calcDate.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				Forecast forecast = ((ForecastEditorInput)getEditorInput()).getForecast();
				BigDecimal value = forecast.getValue(calcDate.getDay());
				resultText.setText(PeanutsUtil.formatQuantity(value));
			}
		});

		label = new Label(top2, SWT.NONE);
		label.setText("Valuel:");
		resultText = new Text(top2, SWT.BORDER);
		resultText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		section2.setClient(top2);

		managedForm.refresh();
	}

	private void setValues() {
		Forecast forecast = ((ForecastEditorInput)getEditorInput()).getForecast();
		forecastName.setText(forecast.getName());
		startDate.setDay(forecast.getStartDay());
		startAmount.setText(PeanutsUtil.formatCurrency(forecast.getStartAmount(), null));
		annualIncrease.setText(PeanutsUtil.formatCurrency(forecast.getAnnualIncrease(), null));
		annualIncreasePercent.setText(PeanutsUtil.formatQuantity(forecast.getAnnualPercent()));
	}

	@Override
	public void setFocus() {
		forecastName.setFocus();
	}

	@Override
	public boolean isDirty() {
		return managedForm.isDirty();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			Forecast forecast = ((ForecastEditorInput)getEditorInput()).getForecast();
			forecast.setName(forecastName.getText());
			forecast.setStartDay(startDate.getDay());
			if (StringUtils.isNotBlank(startAmount.getText())) {
				forecast.setStartAmount(PeanutsUtil.parseCurrency(startAmount.getText()));
			}
			if (StringUtils.isNotBlank(annualIncrease.getText())) {
				forecast.setAnnualIncrease(PeanutsUtil.parseCurrency(annualIncrease.getText()));
			}
			if (StringUtils.isNotBlank(annualIncreasePercent.getText())) {
				forecast.setAnnualPercent(PeanutsUtil.parseCurrency(annualIncreasePercent.getText()));
			}
			managedForm.commit(true);
		} catch (ParseException e) {
			IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage());
			ErrorDialog.openError(getSite().getShell(), "Error creating nested editor", null, status);
		}
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
