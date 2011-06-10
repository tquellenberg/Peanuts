package de.tomsplayground.peanuts.scraping;

import groovy.lang.GroovyClassLoader;

import java.io.IOException;
import java.net.URL;

import org.codehaus.groovy.control.CompilationFailedException;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import de.tomsplayground.peanuts.domain.process.Price;

public class Scraping {

	private final String url;
	
	public Scraping(String url) {
		this.url = url;
	}
	
	public Price scrap(String code) throws CompilationFailedException, InstantiationException, IllegalAccessException, IOException {
		HtmlCleaner cleaner = new HtmlCleaner();
		TagNode node = cleaner.clean(new URL(url));

		ClassLoader parent = getClass().getClassLoader();
		GroovyClassLoader loader = new GroovyClassLoader(parent);
		@SuppressWarnings("unchecked")
		Class<IScraping> groovyClass = loader.parseClass(code);
		IScraping scrapping = groovyClass.newInstance();
		return scrapping.doit(node);
	}

}
