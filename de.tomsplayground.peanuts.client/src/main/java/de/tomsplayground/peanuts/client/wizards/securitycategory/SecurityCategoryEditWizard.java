package de.tomsplayground.peanuts.client.wizards.securitycategory;

import org.eclipse.jface.wizard.Wizard;

import de.tomsplayground.peanuts.domain.statistics.SecurityCategoryMapping;

public class SecurityCategoryEditWizard extends Wizard {

	private final SecurityCategoryMapping mapping;
	private final String category;
	private SecurityCategoryEditWizardPage page;

	public SecurityCategoryEditWizard(SecurityCategoryMapping mapping, String category) {
		this.mapping = mapping;
		this.category = category;
	}

	@Override
	public void addPages() {
		super.addPages();
		page = new SecurityCategoryEditWizardPage("Edit", mapping, category);
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		if (category != null && ! page.getName().equals(category))
			mapping.renameCategory(category, page.getName());
		mapping.setSecuritiesForCategory(page.getName(), page.getSecurities());
		return true;
	}

}
