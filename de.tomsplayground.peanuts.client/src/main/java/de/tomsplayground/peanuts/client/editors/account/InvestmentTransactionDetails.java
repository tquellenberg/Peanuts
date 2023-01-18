package de.tomsplayground.peanuts.client.editors.account;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Currency;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.client.widgets.CalculatorText;
import de.tomsplayground.peanuts.client.widgets.CategoryComposite;
import de.tomsplayground.peanuts.client.widgets.CurrencyComboViewer;
import de.tomsplayground.peanuts.client.widgets.DateComposite;
import de.tomsplayground.peanuts.client.widgets.SecurityProposalText;
import de.tomsplayground.peanuts.domain.base.Account;
import de.tomsplayground.peanuts.domain.base.Category;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.currenncy.CurrencyConverter;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction;
import de.tomsplayground.peanuts.domain.process.InvestmentTransaction.Type;
import de.tomsplayground.peanuts.domain.process.Transaction;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class InvestmentTransactionDetails implements ITransactionDetail {

	private final Account account;
	InvestmentTransaction transaction;
	Transaction parentTransaction;

	private SecurityProposalText securityProposalText;
	private DateComposite date;
	private Text memo;
	private CategoryComposite categoryComposite;
	private Text quantity;
	private Text price;
	private Text commission;
	private Text amount;
	private Button cancel;
	private Button okay;
	private Composite detailComposit;
	private Combo transactionType;
	private boolean internalUpdate;

	private Currency currentSelectedCurrency;

	private final ModifyListener modifyListener = new ModifyListener() {
		@Override
		public void modifyText(ModifyEvent arg0) {
			if ( !internalUpdate) {
				cancel.setEnabled(true);
				okay.setEnabled(true);
			}
		}
	};

	private final ModifyListener recalculateAmount = new ModifyListener() {
		@Override
		public void modifyText(ModifyEvent arg0) {
			if ( !internalUpdate) {
				try {
					Type type = getTransactionType();
					BigDecimal newQuantity = PeanutsUtil.parseQuantity(quantity.getText());
					BigDecimal newPrice = PeanutsUtil.parseCurrency(price.getText());
					BigDecimal newCommission = PeanutsUtil.parseCurrency(commission.getText());
					BigDecimal newAmount = InvestmentTransaction.calculateAmount(type, newPrice, newQuantity, newCommission);
					amount.setText(PeanutsUtil.formatCurrency(newAmount, currencyCombo.getSelectedCurrency()));
				} catch (ParseException e) {
					amount.setText("");
				}
			}
		}
	};
	private CurrencyComboViewer currencyCombo;

	public InvestmentTransactionDetails(Account account) {
		this.account = account;
	}

	@Override
	public Composite createComposite(Composite parent) {
		detailComposit = new Composite(parent, SWT.NONE);
		detailComposit.setLayout(new GridLayout());

		Group group = new Group(detailComposit, SWT.NONE);
		group.setText("Transaction details");
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		securityProposalText = new SecurityProposalText(group);
		securityProposalText.getText().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		date = new DateComposite(group, SWT.NONE);

		memo = new Text(group, SWT.SINGLE | SWT.BORDER);
		GridData ldata = new GridData(SWT.FILL, SWT.CENTER, true, false);
		memo.setLayoutData(ldata);

		categoryComposite = new CategoryComposite(group, SWT.NONE);
		categoryComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Composite box = new Composite(group, SWT.NONE);
		GridLayout gridLayout = new GridLayout(5, false);
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		gridLayout.verticalSpacing = 0;
		box.setLayout(gridLayout);

		createSmallLabel(box, "Type");
		createSmallLabel(box, "Amount");
		createSmallLabel(box, "Price");
		createSmallLabel(box, "Commission");
		createSmallLabel(box, "Total");

		transactionType = new Combo(box, SWT.READ_ONLY);
		transactionType.add("Sell");
		transactionType.add("Buy");
		transactionType.add("Expense");
		transactionType.add("Income");

		quantity = new Text(box, SWT.SINGLE | SWT.BORDER);
		quantity.setLayoutData(new GridData(100, SWT.DEFAULT));
		price = (new CalculatorText(box, SWT.SINGLE | SWT.BORDER)).getText();
		price.setLayoutData(new GridData(150, SWT.DEFAULT));
		commission = (new CalculatorText(box, SWT.SINGLE | SWT.BORDER)).getText();
		commission.setLayoutData(new GridData(150, SWT.DEFAULT));
		amount = new Text(box, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		amount.setLayoutData(new GridData(150, SWT.DEFAULT));

		currencyCombo = new CurrencyComboViewer(group, false, false);
		currencyCombo.getCombo().addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			changeCurrency(currencyCombo.getSelectedCurrency());
		}));
		currencyCombo.getCombo().setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		currencyCombo.selectCurrency(account.getCurrency());
		currentSelectedCurrency = currencyCombo.getSelectedCurrency();

		Composite buttons = new Composite(detailComposit, SWT.NONE);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.END;
		buttons.setLayoutData(gridData);
		buttons.setLayout(new FillLayout(SWT.HORIZONTAL));
		cancel = new Button(buttons, SWT.NONE);
		cancel.setText("Cancel");
		cancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setInput(transaction, parentTransaction);
			}
		});
		okay = new Button(buttons, SWT.NONE);
		okay.setText("Save");
		okay.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (! currentSelectedCurrency.equals(account.getCurrency())) {
					changeCurrency(account.getCurrency());
					currencyCombo.selectCurrency(account.getCurrency());
				}
				readForm();
				cancel.setEnabled(false);
				okay.setEnabled(false);
			}
		});

		quantity.addModifyListener(recalculateAmount);
		quantity.addModifyListener(modifyListener);
		price.addModifyListener(recalculateAmount);
		price.addModifyListener(modifyListener);
		commission.addModifyListener(recalculateAmount);
		commission.addModifyListener(modifyListener);
		transactionType.addModifyListener(recalculateAmount);
		transactionType.addModifyListener(modifyListener);

		categoryComposite.addModifyListener(modifyListener);
		memo.addModifyListener(modifyListener);
		date.addModifyListener(modifyListener);
		securityProposalText.getText().addModifyListener(modifyListener);

		return detailComposit;
	}

	private void changeCurrency(Currency newCurrency) {
		CurrencyConverter converter = Activator.getDefault().getExchangeRates()
			.createCurrencyConverter(currentSelectedCurrency, newCurrency);

		Day transcationDate = date.getDay();
		BigDecimal quantityValue = null;
		try {
			quantityValue = PeanutsUtil.parseCurrency(quantity.getText());
		} catch (ParseException e) {
		}
		BigDecimal priceValue = convertField(price, converter, transcationDate);
		BigDecimal commissionValue = convertField(commission, converter, transcationDate);
		if (quantityValue != null && priceValue != null && commissionValue != null) {
			BigDecimal amountValue = InvestmentTransaction.calculateAmount(getTransactionType(), priceValue, quantityValue, commissionValue);
			amount.setText(PeanutsUtil.formatCurrency(amountValue, newCurrency));
		} else {
			amount.setText("");
		}
		currentSelectedCurrency = newCurrency;
	}

	private BigDecimal convertField(Text field, CurrencyConverter converter, Day newDate) {
		try {
			BigDecimal value = PeanutsUtil.parseCurrency(field.getText());
			value = converter.convert(value, newDate);
			field.setText(PeanutsUtil.formatCurrency(value, null));
			return value;
		} catch (ParseException e) {
		}
		return null;
	}

	private void createSmallLabel(Composite box, String text) {
		Label label = new Label(box, SWT.NONE);
		label.setText(text);
		label.setFont(Activator.getDefault().getSmallFont());
	}

	@Override
	public void dispose() {
		securityProposalText.dispose();
	}

	protected void readForm() {
		try {
			String securityName = securityProposalText.getText().getText();
			Security newSecurity = Activator.getDefault().getAccountManager().getOrCreateSecurity(
				securityName);
			Category newCategory = categoryComposite.getCategory();
			Day newDate = date.getDay();
			BigDecimal newQuantity = PeanutsUtil.parseQuantity(quantity.getText());
			BigDecimal newPrice = PeanutsUtil.parseCurrency(price.getText());
			BigDecimal newCommission = PeanutsUtil.parseCurrency(commission.getText());
			Type type = getTransactionType();
			if (transaction == null) {
				transaction = new InvestmentTransaction(newDate, newSecurity, newPrice,
					newQuantity, newCommission, type);
				transaction.setMemo(memo.getText());
				transaction.setCategory(newCategory);
				if (parentTransaction != null) {
					parentTransaction.addSplit(transaction);
				} else {
					account.addTransaction(transaction);
				}
			} else {
				transaction.setSecurity(newSecurity);
				transaction.setInvestmentDetails(type, newPrice, newQuantity, newCommission);
				transaction.setMemo(memo.getText());
				transaction.setCategory(newCategory);
				transaction.setDay(newDate);
			}
		} catch (ParseException e) {
			IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage());
			(new ErrorDialog(detailComposit.getShell(), "Format Error", "Input not valid.", status,
				IStatus.ERROR)).open();
		}
	}

	private Type getTransactionType() {
		Type type;
		if (transactionType.getSelectionIndex() == 0) {
			type = InvestmentTransaction.Type.SELL;
		} else if (transactionType.getSelectionIndex() == 1) {
			type = InvestmentTransaction.Type.BUY;
		} else if (transactionType.getSelectionIndex() == 2) {
			type = InvestmentTransaction.Type.EXPENSE;
		} else {
			type = InvestmentTransaction.Type.INCOME;
		}
		return type;
	}

	@Override
	public void setInput(Transaction transaction, Transaction parentTransaction) {
		internalUpdate = true;
		this.transaction = (InvestmentTransaction) transaction;
		this.parentTransaction = parentTransaction;
		this.currentSelectedCurrency = account.getCurrency();
		this.currencyCombo.selectCurrency(account.getCurrency());
		if (transaction == null) {
			date.setDay(Day.today());
			memo.setText("");
			categoryComposite.setCategory(null);
			securityProposalText.getText().setText("");
			quantity.setText("");
			price.setText("");
			commission.setText("");
			amount.setText("");
			transactionType.select(0);
		} else {
			date.setDay(transaction.getDay());
			memo.setText(transaction.getMemo() != null ? transaction.getMemo() : "");
			categoryComposite.setCategory(transaction.getCategory());
			if (this.transaction.getSecurity() != null) {
				securityProposalText.getText().setText("");
				securityProposalText.getText().setText(this.transaction.getSecurity().getName());
			} else {
				securityProposalText.getText().setText("");
			}
			if (this.transaction.getQuantity() != null) {
				quantity.setText(PeanutsUtil.formatQuantity(this.transaction.getQuantity()));
			} else {
				quantity.setText("");
			}
			if (this.transaction.getPrice() != null) {
				price.setText(PeanutsUtil.formatCurrency(this.transaction.getPrice(), null));
			} else {
				price.setText("");
			}
			if (this.transaction.getCommission() != null) {
				commission.setText(PeanutsUtil.formatCurrency(this.transaction.getCommission(),
					null));
			} else {
				commission.setText("");
			}
			amount.setText(PeanutsUtil.formatCurrency(transaction.getAmount(), currentSelectedCurrency));
			transactionType.select(switch (this.transaction.getType()) {
				case SELL -> 0;
				case BUY -> 1;
				case EXPENSE -> 2;
				case INCOME -> 3;
			});
			cancel.setEnabled(false);
			okay.setEnabled(false);
		}
		internalUpdate = false;
	}

	@Override
	public Composite getComposite() {
		return detailComposit;
	}

	@Override
	public void setFocus() {
		securityProposalText.getText().setFocus();
	}

}
