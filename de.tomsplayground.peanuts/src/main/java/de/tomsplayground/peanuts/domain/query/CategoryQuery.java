package de.tomsplayground.peanuts.domain.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.tomsplayground.peanuts.domain.base.Category;
import de.tomsplayground.peanuts.domain.process.ITransaction;

@XStreamAlias("categoryQuery")
public class CategoryQuery implements IQuery {

	final private Set<Category> categories = new HashSet<Category>();

	public CategoryQuery(Category cat) {
		if (cat == null) {
			throw new IllegalArgumentException("cat null");
		}
		this.categories.add(cat);
	}

	public CategoryQuery(List<Category> categories) {
		this.categories.addAll(categories);
	}

	public Set<Category> getCategories() {
		return Collections.unmodifiableSet(categories);
	}
	
	private boolean isOkay(Category category) {
		if (category == null) {
			return false;
		}
		return categories.contains(category) || isOkay(category.getParent());
	}

	@Override
	public List<ITransaction> filter(List<ITransaction> trans) {
		List<ITransaction> result = new ArrayList<ITransaction>();
		for (ITransaction transaction : trans) {
			if (isOkay(transaction.getCategory())) {
				result.add(transaction);
			}
		}
		return result;
	}

}
