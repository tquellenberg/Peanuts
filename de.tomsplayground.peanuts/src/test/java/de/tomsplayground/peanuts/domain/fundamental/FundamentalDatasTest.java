package de.tomsplayground.peanuts.domain.fundamental;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.tomsplayground.peanuts.domain.currenncy.Currencies;
import de.tomsplayground.util.Day;

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
		assertNull(fundamentalDatas.getFundamentalData(new Day(2015, 11, 31)));
		assertEquals(datas.get(0), fundamentalDatas.getFundamentalData(new Day(2016, 0 ,1)));
		assertEquals(datas.get(2), fundamentalDatas.getFundamentalData(new Day(2018, 11, 31)));
		assertNull(fundamentalDatas.getFundamentalData(new Day(2019, 11, 31)));
	}

	@Test
	public void testContinuousPE() {
		// 1.7.2017
		assertEquals(new BigDecimal("3.0"), fundamentalDatas.getContinuousEarnings(new Day(2017, 6, 1)).setScale(1, RoundingMode.HALF_UP));
		// 1.1.2018
		assertEquals(new BigDecimal("4.0"), fundamentalDatas.getContinuousEarnings(new Day(2018, 0, 1)).setScale(1, RoundingMode.HALF_UP));
		// 1.7.2018
		assertEquals(new BigDecimal("4.5"), fundamentalDatas.getContinuousEarnings(new Day(2018, 6, 1)).setScale(1, RoundingMode.HALF_UP));
	}

	@Test
	public void testContinuousPEBorder() {
		// 31.12.2014
		assertNull(fundamentalDatas.getContinuousEarnings(new Day(2015, 11, 31)));
		// 1.1.2016
		assertEquals(new BigDecimal("2.0"), fundamentalDatas.getContinuousEarnings(new Day(2016, 0, 1)).setScale(1, RoundingMode.HALF_UP));
		// 1.7.2016
		assertEquals(new BigDecimal("2.0"), fundamentalDatas.getContinuousEarnings(new Day(2016, 6, 1)).setScale(1, RoundingMode.HALF_UP));
		// 1.1.2019
		assertEquals(new BigDecimal("5.0"), fundamentalDatas.getContinuousEarnings(new Day(2019, 0, 1)).setScale(1, RoundingMode.HALF_UP));
		// 1.1.2020
		assertNull(fundamentalDatas.getContinuousEarnings(new Day(2020, 0, 1)));
	}

}
