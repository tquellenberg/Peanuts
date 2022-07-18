package de.tomsplayground.peanuts.domain.base;

import java.util.Comparator;

public interface INamedElement {

	static final Comparator<INamedElement> NAMED_ELEMENT_ORDER = new Comparator<>() {
		public int compare(INamedElement e1, INamedElement e2) {
			return String.CASE_INSENSITIVE_ORDER.compare(e1.getName(), e2.getName());
		}
	};

	String getName();
}
