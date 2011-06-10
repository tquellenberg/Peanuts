package de.tomsplayground.peanuts.domain.process

import de.tomsplayground.util.Day;

class PriceBuilder {

	def day = new Day()
	BigDecimal open
	BigDecimal close
	BigDecimal high
	BigDecimal low
	
	def set(Map args) {
		args.each { 
			if (it.key == "open") this.open = new BigDecimal(it.value.toString())
			if (it.key == "close") this.close = new BigDecimal(it.value.toString())
			if (it.key == "high") this.high = new BigDecimal(it.value.toString())
			if (it.key == "low") this.low = new BigDecimal(it.value.toString())
		}
		return this
	}
		
	def please(action) {
		[the: { what ->
			[of: { n -> action(what(n)) }]
		}]
	}
	
	def set(which) {
		[closex: [with: { println it}]].get(which)
	}
	
	def build() {
		return new Price(day, open, close, high, low)
	}
	
}
