package de.tomsplayground.peanuts.client.watchlist;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.statistics.SecurityCategoryMapping;
import de.tomsplayground.peanuts.domain.watchlist.CategoryFilter;
import de.tomsplayground.peanuts.domain.watchlist.ISecuriityFilter;
import de.tomsplayground.peanuts.domain.watchlist.WatchlistConfiguration;
import de.tomsplayground.peanuts.domain.watchlist.WatchlistConfiguration.Type;

public class WatchlistConfigurationDialog extends Dialog {

	private final WatchlistConfiguration watchlistConfiguration;
	private Text name;
	private Combo typeCombo;
	private Combo filterCombo1;
	private org.eclipse.swt.widgets.List filterValueList;

	public WatchlistConfigurationDialog(Shell parentShell, WatchlistConfiguration watchlistConfiguration) {
		super(parentShell);
		this.watchlistConfiguration = watchlistConfiguration;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Edit watch list");
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		((GridLayout) composite.getLayout()).numColumns = 2;
		((GridLayout) composite.getLayout()).makeColumnsEqualWidth = false;

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.widthHint = convertHorizontalDLUsToPixels(300);
		composite.setLayoutData(gd);

		Label label = new Label(composite, SWT.NONE);
		label.setText("Name");
		label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		name = new Text(composite, SWT.SINGLE | SWT.BORDER);
		name.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		label = new Label(composite, SWT.NONE);
		label.setText("Type");
		label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		typeCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		for (Type t : WatchlistConfiguration.Type.values()) {
			typeCombo.add(t.name());
		}


		label = new Label(composite, SWT.NONE);
		label.setText("Filter");
		label.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));

		final Composite filterCombos = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.horizontalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		filterCombos.setLayout(layout);
		GridData layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		filterCombos.setLayoutData(layoutData);

		List<SecurityCategoryMapping> securityCategoryMappings = Activator.getDefault().getAccountManager().getSecurityCategoryMappings();
		filterCombo1 = new Combo(filterCombos, SWT.DROP_DOWN | SWT.READ_ONLY);
		GridData gridData = new GridData(SWT.LEFT, SWT.TOP, false, false);
		filterCombo1.setLayoutData(gridData);
		filterValueList = new org.eclipse.swt.widgets.List(filterCombos, SWT.READ_ONLY | SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
		gridData.heightHint = 100;
		filterValueList.setLayoutData(gridData);
		for (SecurityCategoryMapping securityCategoryMapping : securityCategoryMappings) {
			filterCombo1.add(securityCategoryMapping.getName());
		}
		filterCombo1.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				final String name = ((Combo)e.widget).getText();
				SecurityCategoryMapping securityCategoryMapping = getSecurityMappingByname(name);
				if (securityCategoryMapping != null) {
					filterValueList.setItems(securityCategoryMapping.getCategories().toArray(new String[0]));
					filterCombos.layout();
				}
			}
		});

		name.setText(watchlistConfiguration.getName());
		typeCombo.select(watchlistConfiguration.getType().ordinal());
		ImmutableList<ISecuriityFilter> filters = watchlistConfiguration.getFilters();
		if (! filters.isEmpty() && filters.get(0) instanceof CategoryFilter filter) {
			filterCombo1.select(ArrayUtils.indexOf(filterCombo1.getItems(), filter.getCategoryName()));

			int selection[] = new int[filter.getCategoryValues().size()];
			int i = 0;
			for (String value : filter.getCategoryValues()) {
				selection[i++] = ArrayUtils.indexOf(filterValueList.getItems(), value);
			}
			filterValueList.select(selection);
		}

		return composite;
	}

	private SecurityCategoryMapping getSecurityMappingByname(final String name) {
		return Iterables.find(Activator.getDefault().getAccountManager().getSecurityCategoryMappings(), new Predicate<SecurityCategoryMapping>() {
			@Override
			public boolean apply(SecurityCategoryMapping input) {
				return input.getName().equals(name);
			}
		}, null);
	}

	@Override
	protected void okPressed() {
		watchlistConfiguration.setName(name.getText());
		Type type = WatchlistConfiguration.Type.values()[typeCombo.getSelectionIndex()];
		watchlistConfiguration.setType(type);
		if (type == Type.MANUAL) {
			watchlistConfiguration.setFilters(Collections.<ISecuriityFilter>emptyList());
		} else {
			String filterName = filterCombo1.getText();
			Set<String> filterValues = Sets.newHashSet(filterValueList.getSelection());
			if (StringUtils.isNoneEmpty(filterName) && ! filterValues.isEmpty()) {
				watchlistConfiguration.setFilters(Lists.<ISecuriityFilter>newArrayList(new CategoryFilter(filterName, filterValues)));
			} else {
				watchlistConfiguration.setFilters(Collections.<ISecuriityFilter>emptyList());
			}
		}
		super.okPressed();
	}

}
