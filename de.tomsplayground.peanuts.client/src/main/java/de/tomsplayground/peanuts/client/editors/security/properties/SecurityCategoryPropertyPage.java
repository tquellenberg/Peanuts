package de.tomsplayground.peanuts.client.editors.security.properties;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PropertyPage;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.statistics.SecurityCategoryMapping;

public class SecurityCategoryPropertyPage extends PropertyPage {
	
	private final List<Combo> combos = new ArrayList<Combo>();
	
	public SecurityCategoryPropertyPage() {
		noDefaultAndApplyButton();
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent,SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		
		IAdaptable adapter = getElement();
		Security security = (Security)adapter.getAdapter(Security.class);

		AccountManager accountManager = Activator.getDefault().getAccountManager();		
		List<SecurityCategoryMapping> securityCategoryMappings = accountManager.getSecurityCategoryMappings();
		for (SecurityCategoryMapping securityCategoryMapping : securityCategoryMappings) {
			Label label = new Label(composite, SWT.NONE);
			label.setText(securityCategoryMapping.getName());

			Combo combo = new Combo(composite, SWT.DROP_DOWN);
			combo.setItems(securityCategoryMapping.getCategories().toArray(new String[0]));
			combo.setData(securityCategoryMapping);
			String category = securityCategoryMapping.getCategory(security);
			if (category != null) {
				combo.setText(category);
			}
			combos.add(combo);
		}
		
		return composite;
	}

	@Override
	public boolean performOk() {
		IAdaptable adapter = getElement();
		Security security = (Security)adapter.getAdapter(Security.class);
		for (Combo combo : combos) {
			SecurityCategoryMapping securityCategoryMapping = (SecurityCategoryMapping) combo.getData();
			String category = combo.getText();
			if (StringUtils.isNotBlank(category)) {
				securityCategoryMapping.setCategory(security, category.trim());
			} else {
				securityCategoryMapping.setCategory(security, null);
			}
		}
		return super.performOk();
	}

}
