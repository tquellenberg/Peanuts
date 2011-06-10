package de.tomsplayground.peanuts.scraping

import org.htmlcleaner.HtmlCleaner;

import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.process.PriceBuilder;
import de.tomsplayground.peanuts.scraping.IScraping;
import de.tomsplayground.util.Day;

class ScrapingTest extends GroovyTestCase {

	def address = "file:src/test/groovy/de/tomsplayground/peanuts/scraping/showpage.asp.html"

	void testSimple() throws Exception {
		def code = "import de.tomsplayground.peanuts.scraping.IScraping;" +
					"import de.tomsplayground.peanuts.domain.process.Price;"+
					"class test implements IScraping { Price doit(org.htmlcleaner.TagNode nodes) {}}"
		new Scraping(address).scrap code
	}
	
	void testXmarkets() {
		def code = "import de.tomsplayground.peanuts.scraping.IScraping;\n" +
					"import de.tomsplayground.peanuts.domain.process.Price;"+
					"import de.tomsplayground.peanuts.domain.process.PriceBuilder;\n"+
					"class test implements IScraping { \n"+
					"   Price doit(org.htmlcleaner.TagNode node) {\n"+
					"		def nodes = node.getElementsByAttValue(\"item\", \"D1DEDE4VS7=DBBL\", true, true)\n"+
					"		def p = new PriceBuilder()\n"+
					"		for (n in nodes) {\n"+
					"			println n.getAttributeByName(\"field\") + \"  \" + n.getText()\n"+
					"			if (n.getAttributeByName(\"field\") == \"bid\") { \n"+
					"				p.set(close: n.getText().toString().replace(',','.'))\n"+
					"			}\n"+
					"			if (n.getAttributeByName(\"field\") == \"dailyhigh\") { \n"+
					"				p.set(high: n.getText().toString().replace(',','.'))\n"+
					"			}\n"+
					"			if (n.getAttributeByName(\"field\") == \"dailylow\") { \n"+
					"				p.set(low: n.getText().toString().replace(',','.'))\n"+
					"			}\n"+
					"		}\n"+
					"		return p.build()\n"+
					"   }\n"+
					"}"
		def price = new Scraping(address).scrap(code)
		assertEquals "137.710" as BigDecimal, price.close
	}
	
}
