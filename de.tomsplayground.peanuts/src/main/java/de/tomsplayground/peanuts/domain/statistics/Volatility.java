package de.tomsplayground.peanuts.domain.statistics;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.domain.process.IPrice;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class Volatility {

	private final static Logger log = LoggerFactory.getLogger(Volatility.class);

	public double calculateVolatility(IPriceProvider pp) {
		Day maxDate = pp.getMaxDate();
		if (maxDate == null) {
			return 0.0;
		}
		Day minDate = maxDate.addYear(-1);
		ImmutableList<BigDecimal> values = pp.getPrices(minDate, maxDate).stream()
				.map(IPrice::getValue)
				.collect(ImmutableList.toImmutableList());
		if (values.size() < 3) {
			return 0.0;
		}
		return calculateVolatility(values);
	}

	public double calculateVolatility(ImmutableList<BigDecimal> values) {
		log.debug("Size: {}", values.size());
		BigDecimal v1 = values.get(0);
		double yiel[] = new double[values.size() - 1];
		int i = 0;
		for (BigDecimal v2 : values.subList(1, values.size())) {
			yiel[i] = Math.log(v2.divide(v1, PeanutsUtil.MC).doubleValue());
			if (log.isDebugEnabled()) {
				log.debug("V1:" + v1 + " V2:" + v2 + " Y:" + yiel[i]);
			}
			i++;
			v1 = v2;
		}
		double avg = 0;
		for (double r : yiel) {
			avg += r;
		}
		avg = avg / yiel.length;
		log.debug("AVG: {}", avg);
		double volatility = 0;
		for (double r : yiel) {
			double d = (r - avg);
			volatility += (d * d);
		}
		log.debug("Vola1: {}", volatility);
		if (volatility < 0.001) {
			return 0.0;
		}
		volatility = Math.sqrt((volatility * 252) / (yiel.length - 1));
		log.debug("Vola2: {}", volatility);
		return volatility;
	}

}
