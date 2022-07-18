package de.tomsplayground.peanuts.domain.base;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

public class NameTest {
	
	private Security security1;
	private Security security2;
	private Security security3;

	@Before
	public void setup() {
		security1 = new Security("Publicis Groupe");
		security2 = new Security("Public Storage");
		security3 = new Security("eBay");
	}
	
	@Test
	public void testWithespaceSorting() {
// Collator ignore whitespaces !!!!
//		
//		Collator collator = Collator.getInstance(Locale.GERMANY);
//		int result1 = collator.compare(security1.getName(), security2.getName());
//		System.out.println(result1);

		int compare = INamedElement.NAMED_ELEMENT_ORDER.compare(security1, security2);
		assertTrue(compare > 0);

		List<Security> list = Lists.newArrayList(security1, security2, security3);
		list.sort(INamedElement.NAMED_ELEMENT_ORDER);
		System.out.println(list);
	}
	
	@Test
	public void testUperAndLowerCase() {
		int compare = INamedElement.NAMED_ELEMENT_ORDER.compare(security1, security3);
		assertTrue(compare > 0);
	}

}
