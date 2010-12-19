package de.tomsplayground.peanuts.client.wizards.credit;

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
import de.tomsplayground.peanuts.domain.process.Credit;
import de.tomsplayground.peanuts.domain.process.ICredit;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class BasePage extends WizardPage {

	private Text creditName;
	private DateComposite startDate;
	private DateComposite endDate;
	private Text amount;
	private Text interestRate;
	private Text payment;

	protected BasePage(String pageName) {
		super(pageName);
		setTitle("Credit");
		setMessage("Basic information for a credit.");
		setDescription("Credit");
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite top = new Composite(parent, SWT.NONE);
		top.setLayout(new GridLayout(2, false));

		Label label = new Label(top, SWT.NONE);
		label.setText("Credit name:");
		creditName = new Text(top, SWT.BORDER);
		creditName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setPageComplete(StringUtils.isNotBlank(((Text)e.getSource()).getText()));
			}
		});
		creditName.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		
		label = new Label(top, SWT.NONE);
		label.setText("From:");
		startDate = new DateComposite(top, SWT.NONE);
		
		label = new Label(top, SWT.NONE);
		label.setText("To:");
		endDate = new DateComposite(top, SWT.NONE);
		
		label = new Label(top, SWT.NONE);
		label.setText("Amount:");
		amount = (new CalculatorText(top, SWT.SINGLE | SWT.BORDER)).getText();
		amount.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		label = new Label(top, SWT.NONE);
		label.setText("Annual interest rate:");
		interestRate = (new CalculatorText(top, SWT.SINGLE | SWT.BORDER)).getText();
		interestRate.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		label = new Label(top, SWT.NONE);
		label.setText("Payment:");
		payment = (new CalculatorText(top, SWT.SINGLE | SWT.BORDER)).getText();
		payment.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		setControl(top);
	}

	public ICredit getCredit() {
		try {
			BigDecimal a = BigDecimal.ZERO;
			if (StringUtils.isNotBlank(amount.getText()))
				a = PeanutsUtil.parseCurrency(amount.getText());
			BigDecimal i = BigDecimal.ZERO;
			if (StringUtils.isNotBlank(interestRate.getText()))
				i = PeanutsUtil.parseCurrency(interestRate.getText());
			BigDecimal p = BigDecimal.ZERO;
			if (StringUtils.isNotBlank(payment.getText()))
				p = PeanutsUtil.parseCurrency(payment.getText());

			Credit credit = new Credit(startDate.getDay(), endDate.getDay(), a, i);
			credit.setName(creditName.getText());
			credit.setPayment(p);
			return credit;
		} catch (ParseException e) {
			setErrorMessage(e.getMessage());
			return null;
		}
	}

}
