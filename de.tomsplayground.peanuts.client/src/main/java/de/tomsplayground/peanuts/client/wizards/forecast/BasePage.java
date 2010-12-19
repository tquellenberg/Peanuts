package de.tomsplayground.peanuts.client.wizards.forecast;

import java.math.BigDecimal;
import java.text.ParseException;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.tomsplayground.peanuts.client.widgets.CalculatorText;
import de.tomsplayground.peanuts.client.widgets.DateComposite;
import de.tomsplayground.peanuts.domain.reporting.forecast.Forecast;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class BasePage extends WizardPage {

	private Text forecastName;
	private DateComposite startDate;
	private Text startAmount;
	private Text annualIncrease;
	private Text annualIncreasePercent;

	protected BasePage(String pageName) {
		super(pageName);
		setTitle("Forecast");
		setMessage("Basic information for a forecast.");
		setDescription("Forecast");
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite top = new Composite(parent, SWT.NONE);
		top.setLayout(new GridLayout(2, false));

		Label label = new Label(top, SWT.NONE);
		label.setText("Forecast name:");
		forecastName = new Text(top, SWT.BORDER);
		forecastName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setPageComplete(StringUtils.isNotBlank(((Text)e.getSource()).getText()));
			}
		});
		forecastName.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		
		label = new Label(top, SWT.NONE);
		label.setText("From:");
		startDate = new DateComposite(top, SWT.NONE);
		
		label = new Label(top, SWT.NONE);
		label.setText("Start amount:");
		startAmount = (new CalculatorText(top, SWT.SINGLE | SWT.BORDER)).getText();
		startAmount.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		label = new Label(top, SWT.NONE);
		label.setText("Annual increase (absolut):");
		annualIncrease = (new CalculatorText(top, SWT.SINGLE | SWT.BORDER)).getText();
		annualIncrease.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		label = new Label(top, SWT.NONE);
		label.setText("Annual increase (percent):");
		annualIncreasePercent = (new CalculatorText(top, SWT.SINGLE | SWT.BORDER)).getText();
		annualIncreasePercent.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		setControl(top);
	}

	public Forecast getForecaste() {
		try {
			BigDecimal amount = BigDecimal.ZERO;
			if (StringUtils.isNotBlank(startAmount.getText()))
				amount = PeanutsUtil.parseCurrency(startAmount.getText());
			BigDecimal increase = BigDecimal.ZERO;
			if (StringUtils.isNotBlank(annualIncrease.getText()))
				increase = PeanutsUtil.parseCurrency(annualIncrease.getText());
			Forecast forecast = new Forecast(startDate.getDay(), amount, increase);
			BigDecimal increasePercent = BigDecimal.ZERO;
			if (StringUtils.isNotBlank(annualIncreasePercent.getText()))
				increasePercent = PeanutsUtil.parseCurrency(annualIncreasePercent.getText());
			forecast.setAnnualPercent(increasePercent);
			forecast.setName(forecastName.getText());
			return forecast;
		} catch (ParseException e) {
			setErrorMessage(e.getMessage());
			return null;
		}
	}

}
