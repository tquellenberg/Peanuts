package de.tomsplayground.peanuts.client.widgets;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.Category;
import de.tomsplayground.peanuts.domain.base.Category.Type;

public class CategoryLabelProvider extends LabelProvider {
	@Override
	public String getText(Object element) {
		if (element instanceof Category cat) {
			return cat.getName();
		}
		return super.getText(element);
	}

	@Override
	public Image getImage(Object element) {
		Type type = null;
		if (element instanceof Category cat) {
			type = cat.getType();
		} else if (element instanceof Category.Type) {
			type = (Category.Type) element;
		}
		if (type != null) {
			if (type == Category.Type.EXPENSE) {
				return Activator.getDefault().getImageRegistry().get(Activator.IMAGE_CATEGORY_EXPENSE);
			}
			if (type == Category.Type.INCOME) {
				return Activator.getDefault().getImageRegistry().get(Activator.IMAGE_CATEGORY_INCOME);
			}
			return Activator.getDefault().getImageRegistry().get(Activator.IMAGE_CATEGORY);
		}
		return super.getImage(element);
	}
}
