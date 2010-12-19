package de.tomsplayground.peanuts.client.widgets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import de.tomsplayground.peanuts.domain.base.Category;

public class CategoryContentProvider implements ITreeContentProvider {

	private Set<Category> categories;

	@Override
	@SuppressWarnings("unchecked")
	public Object[] getElements(Object inputElement) {
		categories = (Set<Category>) inputElement;
		return Category.Type.values();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof Category.Type) {
			Category.Type type = (Category.Type) parentElement;
			List<Category> catList = new ArrayList<Category>();
			for (Category category : categories) {
				if (category.getType() == type) {
					catList.add(category);
				}
			}
			Collections.sort(catList, new Comparator<Category>() {
				@Override
				public int compare(Category o1, Category o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			return catList.toArray();
		}
		if (parentElement instanceof Category) {
			Category category = (Category) parentElement;
			return category.getChildCategories().toArray();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof Category.Type) {
			return true;
		}
		if (element instanceof Category) {
			return !((Category) element).getChildCategories().isEmpty();
		}
		return false;
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// nothing to do
	}

	@Override
	public void dispose() {
		// nothing to do
	}
}
