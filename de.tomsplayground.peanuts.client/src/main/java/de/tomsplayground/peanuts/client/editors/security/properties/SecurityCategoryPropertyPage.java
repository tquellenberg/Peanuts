package de.tomsplayground.peanuts.client.editors.security.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.statistics.SecurityCategoryMapping;

public class SecurityCategoryPropertyPage extends PropertyPage {

	private final static String[] ICONS = new String[]{
		"flag_blue", "flag_green", "flag_orange", "flag_pink",
		"flag_purple", "flag_red", "flag_yellow",
		"star", "stop", "delete", "accept",
		"add", "error", "exclamation", "heart", "new",
		"thumb_up", "thumb_down"
	};

	private final List<Combo> combos = new ArrayList<Combo>();

	private String selectedIcon = "";

	private Text iconTextField;

	private final SelectionAdapter iconButtonListener = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			selectedIcon = Objects.toString(e.widget.getData(), "");
		}
	};

	public SecurityCategoryPropertyPage() {
		noDefaultAndApplyButton();
	}

	@Override
	protected Control createContents(Composite parent) {
		IAdaptable adapter = getElement();
		Security security = (Security)adapter.getAdapter(Security.class);
		AccountManager accountManager = Activator.getDefault().getAccountManager();

		Composite composite = new Composite(parent,SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		Label label1 = new Label(composite, SWT.NONE);
		label1.setText("Icon");
		label1.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

		Composite buttonComposite = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout(7, true);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		buttonComposite.setLayout(layout);

		selectedIcon = StringUtils.defaultString(security.getConfigurationValue("icon"));
		String iconText = StringUtils.defaultString(security.getConfigurationValue("iconText"));
		Button button = new Button(buttonComposite, SWT.RADIO);
		button.setText("None");
		button.setSelection(StringUtils.isEmpty(selectedIcon));
		GridData layoutData = new GridData();
		layoutData.horizontalSpan = 7;
		button.setLayoutData(layoutData);
		button.addSelectionListener(iconButtonListener);
		for (String iconName : ICONS) {
			button = new Button(buttonComposite, SWT.RADIO);
			button.setData(iconName);
			button.setImage(Activator.getDefault().getImage("icons/"+iconName+".png"));
			if (iconName.equals(selectedIcon)) {
				button.setSelection(true);
			}
			button.addSelectionListener(iconButtonListener);
		}

		label1 = new Label(composite, SWT.NONE);
		label1.setText("Icon text");

		iconTextField = new Text(composite, SWT.BORDER);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		iconTextField.setLayoutData(gridData);
		iconTextField.setText(iconText);

		List<SecurityCategoryMapping> securityCategoryMappings = accountManager.getSecurityCategoryMappings();
		for (SecurityCategoryMapping securityCategoryMapping : securityCategoryMappings) {
			Label label = new Label(composite, SWT.NONE);
			label.setText(securityCategoryMapping.getName());

			Combo combo = new Combo(composite, SWT.DROP_DOWN);
			combo.setItems(securityCategoryMapping.getCategories().toArray(new String[0]));
			combo.setData(securityCategoryMapping);
			combo.setText(securityCategoryMapping.getCategory(security));
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
		security.putConfigurationValue("icon", selectedIcon);
		security.putConfigurationValue("iconText", iconTextField.getText());
		return super.performOk();
	}

}
