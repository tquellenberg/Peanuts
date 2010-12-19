package de.tomsplayground.peanuts.client.dnd;

import java.io.Serializable;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.Category;

public class CategoryTransferData implements Serializable, IPeanutsTransferData {

	private static final long serialVersionUID = -6527830881798402363L;

	String categoryPath;

	public CategoryTransferData(Category category) {
		this.categoryPath = category.getPath();
	}

	public Category getCategory() {
		return Activator.getDefault().getAccountManager().getCategoryByPath(categoryPath);
	}

}
