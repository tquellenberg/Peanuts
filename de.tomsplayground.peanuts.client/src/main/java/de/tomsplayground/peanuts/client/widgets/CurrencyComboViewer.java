package de.tomsplayground.peanuts.client.widgets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import de.tomsplayground.peanuts.domain.currenncy.Currencies;

public class CurrencyComboViewer {

	private List<Currency> currencies;
	private Combo combo;
	private boolean optional;

	public CurrencyComboViewer(Composite parent, boolean optional) {
		this.optional = optional;
		combo = new Combo(parent, SWT.READ_ONLY);
		currencies = Lists.newArrayList(Currency.getAvailableCurrencies());
		Collections.sort(currencies, new Comparator<Currency>() {
			@Override
			public int compare(Currency o1, Currency o2) {
				return o1.getDisplayName().compareToIgnoreCase(o2.getDisplayName());
			}
		});

		int index = 0;
		for (Currency c : Currencies.getInstance().getMainCurrencies()) {
			currencies.add(index++, c);
		}

		ArrayList<String> currencyNames = Lists.newArrayList(Iterables.transform(currencies, new Function<Currency, String>() {
			@Override
			public String apply(Currency arg0) {
				return arg0.getDisplayName();
			}
		}));
		if (optional) {
			combo.add("");
		}
		for (String name : currencyNames) {
			combo.add(name);
		}
		if (! optional) {
			selectCurrency(Currencies.getInstance().getDefaultCurrency());
		}
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
