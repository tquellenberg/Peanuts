package de.tomsplayground.peanuts.scraping;

import org.htmlcleaner.TagNode;

import de.tomsplayground.peanuts.domain.process.Price;

public interface IScraping {

	Price doit(TagNode node);
}
