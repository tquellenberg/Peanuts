package de.tomsplayground.peanuts.domain.process

import de.tomsplayground.util.Day;
import groovy.util.GroovyTestCase;

class Price2Test extends GroovyTestCase {

	void testSimple() throws Exception {
		def d = new Day(2011, 11, 31)
		def p = new Price(d, new BigDecimal("10.0"))
		def p2 = new Price(d, "10.0" as BigDecimal)
		println p
		println p2
	}
	
}
