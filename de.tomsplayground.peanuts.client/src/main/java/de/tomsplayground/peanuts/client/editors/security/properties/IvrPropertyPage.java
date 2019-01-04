package de.tomsplayground.peanuts.client.editors.security.properties;

import java.math.BigDecimal;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class IvrPropertyPage extends PropertyPage {

	public static final String IVR_MAX = "IVR_MAX";

	public static final String IVR_MIN = "IVR_MIN";

	public static final String IVR_RANK = "IVR_RANK";

	public static final String IVR_CALCULATION = "IVR_CALCULATION";

	private Text minField;
	private Text maxField;
	private Text rankField;
	private Button calculateField;

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent,SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		Label label = new Label(composite, SWT.NONE);
		label.setText("Calculate IVR");
		calculateField = new Button(composite, SWT.CHECK);

		minField = createTextWithLabel(composite, "Min");
		maxField = createTextWithLabel(composite, "Max");
		rankField = createTextWithLabel(composite, "Rank");

		Security security = getElement().getAdapter(Security.class);
		if (StringUtils.isNotBlank(security.getConfigurationValue(IVR_MIN))) {
			minField.setText(PeanutsUtil.formatQuantity(new BigDecimal(security.getConfigurationValue(IVR_MIN))));
		}
		if (StringUtils.isNotBlank(security.getConfigurationValue(IVR_MAX))) {
			maxField.setText(PeanutsUtil.formatQuantity(new BigDecimal(security.getConfigurationValue(IVR_MAX))));
		}
		if (StringUtils.isNotBlank(security.getConfigurationValue(IVR_RANK))) {
			rankField.setText(PeanutsUtil.formatPercent(new BigDecimal(security.getConfigurationValue(IVR_RANK))));
		}
		if (StringUtils.isNotBlank(security.getConfigurationValue(IVR_CALCULATION))) {
			calculateField.setSelection(Boolean.valueOf(security.getConfigurationValue(IVR_CALCULATION)));
		}

		return composite;
	}

	private Text createTextWithLabel(Composite group, String labelText) {
		Label label = new Label(group, SWT.NONE);
		label.setText(labelText);
		Text t = new Text(group, SWT.BORDER);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		t.setLayoutData(gridData);
		return t;
	}

	@Override
	public boolean performOk() {
		Security security = getElement().getAdapter(Security.class);
		security.putConfigurationValue(IVR_CALCULATION, Boolean.toString(calculateField.getSelection()));
		return true;
	}

}
