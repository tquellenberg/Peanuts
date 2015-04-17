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

public class CurrencyComboViewer {

	private List<Currency> currencies;
	private Combo combo;

	public CurrencyComboViewer(Composite parent, int style) {
		combo = new Combo(parent, SWT.READ_ONLY);
		currencies = Lists.newArrayList(Currency.getAvailableCurrencies());
		Collections.sort(currencies, new Comparator<Currency>() {
			@Override
			public int compare(Currency o1, Currency o2) {
				return o1.getDisplayName().compareToIgnoreCase(o2.getDisplayName());
			}
		});
		currencies.add(0, Currency.getInstance("USD"));
		currencies.add(1, Currency.getInstance("EUR"));
		currencies.add(2, Currency.getInstance("GBP"));
		currencies.add(3, Currency.getInstance("CHF"));
		currencies.add(4, Currency.getInstance("JPY"));

		ArrayList<String> currencyNames = Lists.newArrayList(Iterables.transform(currencies, new Function<Currency, String>() {
			@Override
			public String apply(Currency arg0) {
				return arg0.getDisplayName();
			}
		}));
		for (String name : currencyNames) {
			combo.add(name);
		}
		selectCurrency(Currency.getInstance("EUR"));
	}

	public Combo getCombo() {
		return combo;
	}

	public void selectCurrency(Currency currency) {
		int indexOf = currencies.indexOf(currency);
		if (indexOf >= 0) {
			combo.select(indexOf);
		}
	}

	public Currency getSelectedCurrency() {
		return currencies.get(combo.getSelectionIndex());
	}

}
