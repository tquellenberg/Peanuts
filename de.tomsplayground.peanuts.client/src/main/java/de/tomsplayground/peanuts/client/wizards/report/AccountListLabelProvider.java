package de.tomsplayground.peanuts.client.wizards.report;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import de.tomsplayground.peanuts.domain.base.Account;

public class AccountListLabelProvider extends LabelProvider implements ITableLabelProvider {

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		Account account = (Account) element;
		switch (columnIndex) {
			case 0:
				return account.getName() + " (" + account.getCurrency() + ")";
			default:
				break;
		}
		return "";
	}
}