package de.tomsplayground.peanuts.domain.fundamental;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.tomsplayground.peanuts.domain.currenncy.Currencies;
import de.tomsplayground.peanuts.util.Day;

public class FundamentalDatasTest {

	private FundamentalDatas fundamentalDatas;
	private List<FundamentalData> datas;

	@Before
	public void setup() {
		datas = new ArrayList<>();
		datas.add(createFundamentaData(2016, new BigDecimal("2.0")));
		datas.add(createFundamentaData(2017, new BigDecimal("4.0")));
		datas.add(createFundamentaData(2018, new BigDecimal("5.0")));
		fundamentalDatas = new FundamentalDatas(datas, null);
	}

	private FundamentalData createFundamentaData(int year, BigDecimal pe) {
		FundamentalData fundamentalData = new FundamentalData();
		fundamentalData.setCurrency(Currencies.getInstance().getDefaultCurrency());
		fundamentalData.setYear(year);
		fundamentalData.setEarningsPerShare(pe);
		return fundamentalData;
	}

	@Test
	public void testGetFundamentalData() {
		assertNull(fundamentalDatas.getFundamentalData(Day.lastDayOfYear(2015)));
		assertEquals(datas.get(0), fundamentalDatas.getFundamentalData(Day.firstDayOfYear(2016)));
		assertEquals(datas.get(2), fundamentalDatas.getFundamentalData(Day.lastDayOfYear(2018)));
		assertNull(fundamentalDatas.getFundamentalData(Day.lastDayOfYear(2019)));
	}

	@Test
	public void testContinuousPE() {
		// 1.7.2017
		assertEquals(new BigDecimal("3.0"), fundamentalDatas.getContinuousEarnings(Day.of(2017, Month.JULY, 1)).setScale(1, RoundingMode.HALF_UP));
		// 1.1.2018
		assertEquals(new BigDecimal("4.0"), fundamentalDatas.getContinuousEarnings(Day.firstDayOfYear(2018)).setScale(1, RoundingMode.HALF_UP));
		// 1.7.2018
		assertEquals(new BigDecimal("4.5"), fundamentalDatas.getContinuousEarnings(Day.of(2018, Month.JULY, 1)).setScale(1, RoundingMode.HALF_UP));
	}

	@Test
	public void testContinuousPEBorder() {
		// 31.12.2014
		assertNull(fundamentalDatas.getContinuousEarnings(Day.lastDayOfYear(2015)));
		// 1.1.2016
		assertEquals(new BigDecimal("2.0"), fundamentalDatas.getContinuousEarnings(Day.firstDayOfYear(2016)).setScale(1, RoundingMode.HALF_UP));
		// 1.7.2016
		assertEquals(new BigDecimal("2.0"), fundamentalDatas.getContinuousEarnings(Day.of(2016, Month.JULY, 1)).setScale(1, RoundingMode.HALF_UP));
		// 1.1.2019
		assertEquals(new BigDecimal("5.0"), fundamentalDatas.getContinuousEarnings(Day.firstDayOfYear(2019)).setScale(1, RoundingMode.HALF_UP));
		// 1.1.2020
		assertNull(fundamentalDatas.getContinuousEarnings(Day.firstDayOfYear(2020)));
	}

}
