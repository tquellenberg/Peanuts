package de.tomsplayground.peanuts.client.editors.credit.properties;

import java.text.ParseException;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.widgets.AccountComposite;
import de.tomsplayground.peanuts.client.widgets.CalculatorText;
import de.tomsplayground.peanuts.client.widgets.DateComposite;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.process.Credit;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class CreditPropertyPage extends PropertyPage {

	private Text creditName;
	private DateComposite startDate;
	private DateComposite endDate;
	private Text amount;
	private Text interestRate;
	private Text paymentAmount;
	private Combo paymentInterval;
	private AccountComposite account;

	public CreditPropertyPage() {
		noDefaultAndApplyButton();
	}
	
	@Override
	protected Control createContents(Composite parent) {
		Composite top = new Composite(parent,SWT.NONE);
		top.setLayout(new GridLayout(2, false));
		
		Label label = new Label(top, SWT.NONE);
		label.setText("Credit name:");
		creditName = new Text(top, SWT.BORDER);
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
		paymentAmount = (new CalculatorText(top, SWT.SINGLE | SWT.BORDER)).getText();
		paymentAmount.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		label = new Label(top, SWT.NONE);
		label.setText("Payment interval:");
		paymentInterval = new Combo(top, SWT.READ_ONLY);
		for (Credit.PaymentInterval t : Credit.PaymentInterval.values())
			paymentInterval.add(t.toString());
		
		label = new Label(top, SWT.NONE);
		label.setText("Account:");
		account = new AccountComposite(top, SWT.NONE);
		
		setValues();
		
		return top;
	}

	private void setValues() {
		IAdaptable adapter = getElement();
		Credit credit = (Credit)adapter.getAdapter(Credit.class);

		creditName.setText(credit.getName());
		startDate.setDay(credit.getStart());
		endDate.setDay(credit.getEnd());
		amount.setText(PeanutsUtil.formatCurrency(credit.getAmount(), null));
		interestRate.setText(PeanutsUtil.formatCurrency(credit.getInterestRate(), null));
		paymentAmount.setText(PeanutsUtil.formatQuantity(credit.getPaymentAmount()));
		paymentInterval.setText(credit.getPaymentInterval()==null?"":credit.getPaymentInterval().toString());
		Account connectedAccount = credit.getConnection();
		if (connectedAccount != null) {
			account.setAccount(connectedAccount);
		}
	}
	
	@Override
	public boolean performOk() {
		try {
			IAdaptable adapter = getElement();
			Credit credit = (Credit)adapter.getAdapter(Credit.class);
			credit.setName(creditName.getText());
			credit.setStart(startDate.getDay());
			credit.setEnd(endDate.getDay());
			if (StringUtils.isNotBlank(amount.getText()))
				credit.setAmount(PeanutsUtil.parseCurrency(amount.getText()));
			if (StringUtils.isNotBlank(interestRate.getText()))
				credit.setInterestRate(PeanutsUtil.parseCurrency(interestRate.getText()));
			if (StringUtils.isNotBlank(paymentAmount.getText()))
				credit.setPayment(PeanutsUtil.parseCurrency(paymentAmount.getText()));
			String typeName = paymentInterval.getItem(paymentInterval.getSelectionIndex());
			credit.setPaymentInterval(Credit.PaymentInterval.valueOf(typeName));
			credit.setConnection(account.getAccount());
			return true;
		} catch (ParseException e) {
			IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage());
			ErrorDialog.openError(getShell(), "Error creating nested editor", null, status);
			return false;
		}
	}

}
