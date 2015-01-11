package de.tomsplayground.peanuts.domain.query;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Predicate;
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
	public Predicate<ITransaction> getPredicate() {
		return new Predicate<ITransaction>() {
			@Override
			public boolean apply(ITransaction input) {
				return isOkay(input.getCategory());
			}
		};
	}

}
