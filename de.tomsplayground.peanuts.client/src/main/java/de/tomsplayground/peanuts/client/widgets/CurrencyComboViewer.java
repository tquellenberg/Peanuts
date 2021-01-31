package de.tomsplayground.peanuts.client.widgets;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.currenncy.Currencies;

public class CurrencyComboViewer {

	private final ImmutableList<Currency> currencies;

	private final Combo combo;

	private final boolean optional;

	private final boolean allCurrencies;

	public CurrencyComboViewer(Composite parent, boolean optional, boolean allCurrencies) {
		this.allCurrencies = allCurrencies;
		this.currencies = getValues();
		this.optional = optional;
		this.combo = new Combo(parent, SWT.READ_ONLY);

		if (optional) {
			combo.add("");
		}

		currencies.stream()
			.map(c -> c.getDisplayName())
			.forEach(name -> combo.add(name));

		if (! optional) {
			selectCurrency(Currencies.getInstance().getDefaultCurrency());
		}
	}

	private ImmutableList<Currency> getValues() {
		List<Currency> currencies = new ArrayList<>();

		if (allCurrencies) {
			currencies.addAll(Currency.getAvailableCurrencies());
			currencies.sort( (a,b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));
		}

		int index = 0;
		for (Currency c : Currencies.getInstance().getCurrenciesWithExchangeSecurity(Activator.getDefault().getAccountManager())) {
			currencies.add(index++, c);
		}

		return ImmutableList.copyOf(currencies);
	}

	public Combo getCombo() {
		return combo;
	}

	public final void selectCurrency(Currency currency) {
		int indexOf = currencies.indexOf(currency);
		if (indexOf >= 0) {
			if (optional) {
				indexOf++;
			}
			combo.select(indexOf);
		}
	}

	public Currency getSelectedCurrency() {
		int selectionIndex = combo.getSelectionIndex();
		if (selectionIndex == -1) {
			return null;
		}
		if (optional) {
			if (selectionIndex == 0) {
				return null;
			}
			selectionIndex--;
		}
		return currencies.get(selectionIndex);
	}

}
