package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;

import org.junit.Test;

import de.tomsplayground.peanuts.Helper;
import de.tomsplayground.peanuts.domain.process.Credit.PaymentInterval;
import de.tomsplayground.peanuts.util.Day;

public class CreditTest {

	private static final Day BEFORE_END_OF_FIRST_YEAR = Day.of(2008, 11, 30);
	private static final Day END_OF_FIRST_YEAR = Day.of(2008, 11, 31);
	private static final Day FIRST_DAY_OF_SECOND_YEAR = Day.of(2009, 0, 1);
	private static final Day FIRST_MONTH_OF_SECOND_YEAR = Day.of(2009, 0, 31);
	private static final Day END_OF_SECOND_YEAR = Day.of(2009, 11, 31);
	private static final Day FIRST_DAY_OF_THIRD_YEAR = Day.of(2010, 0, 1);
	private static final Day END_OF_THIRD_YEAR = Day.of(2010, 11, 31);

	private static final BigDecimal CREDIT_AMOUNT = new BigDecimal("48000.00");
	private static final BigDecimal INTEREST_RATE = new BigDecimal("4.0");

	@Test
	public void testAnnualInterest() throws Exception {
		Credit credit = new Credit(Day.of(2008, 0, 1), END_OF_THIRD_YEAR, CREDIT_AMOUNT, INTEREST_RATE);

		Helper.assertEquals(new BigDecimal("1920"), credit.getInterest(BEFORE_END_OF_FIRST_YEAR));
		Helper.assertEquals(CREDIT_AMOUNT, credit.amount(BEFORE_END_OF_FIRST_YEAR));

		Helper.assertEquals(new BigDecimal("1920"), credit.getInterest(END_OF_FIRST_YEAR));
		Helper.assertEquals(CREDIT_AMOUNT, credit.amount(END_OF_FIRST_YEAR));

		Helper.assertEquals(new BigDecimal("166.40"), credit.getInterest(FIRST_DAY_OF_SECOND_YEAR));
		Helper.assertEquals(new BigDecimal("49920"), credit.amount(FIRST_DAY_OF_SECOND_YEAR));

		Helper.assertEquals(new BigDecimal("166.40"), credit.getInterest(FIRST_MONTH_OF_SECOND_YEAR));
		Helper.assertEquals(new BigDecimal("49920"), credit.amount(FIRST_MONTH_OF_SECOND_YEAR));

		Helper.assertEquals(new BigDecimal("1996.8"), credit.getInterest(END_OF_SECOND_YEAR));
		Helper.assertEquals(new BigDecimal("49920"), credit.amount(END_OF_SECOND_YEAR));

		Helper.assertEquals(new BigDecimal("2076.672"), credit.getInterest(END_OF_THIRD_YEAR));
		Helper.assertEquals(new BigDecimal("51916.8"), credit.amount(END_OF_THIRD_YEAR));
	}

	@Test
	public void testPartialAnnualInterest() throws Exception {
		Credit credit = new Credit(Day.of(2008, 2, 1), END_OF_THIRD_YEAR, CREDIT_AMOUNT, INTEREST_RATE);

		Helper.assertEquals(new BigDecimal("1600"), credit.getInterest(END_OF_FIRST_YEAR));
		Helper.assertEquals(CREDIT_AMOUNT, credit.amount(END_OF_FIRST_YEAR));

		Helper.assertEquals(new BigDecimal("1984"), credit.getInterest(END_OF_SECOND_YEAR));
		Helper.assertEquals(new BigDecimal("49600"), credit.amount(END_OF_SECOND_YEAR));

		Helper.assertEquals(new BigDecimal("2063.36"), credit.getInterest(END_OF_THIRD_YEAR));
		Helper.assertEquals(new BigDecimal("51584"), credit.amount(END_OF_THIRD_YEAR));
	}

	@Test
	public void testPartialAnnualInterest2() throws Exception {
		Credit credit = new Credit(Day.of(2008, 11, 1), END_OF_THIRD_YEAR, CREDIT_AMOUNT, INTEREST_RATE);

		Helper.assertEquals(new BigDecimal("160"), credit.getInterest(END_OF_FIRST_YEAR));
		Helper.assertEquals(CREDIT_AMOUNT, credit.amount(END_OF_FIRST_YEAR));
		Helper.assertEquals(new BigDecimal("48160"), credit.amount(FIRST_DAY_OF_SECOND_YEAR));
	}

	@Test
	public void testMonthlyPayment1Month() throws Exception {
		Credit credit = new Credit(Day.of(2008, 11, 1), END_OF_THIRD_YEAR, CREDIT_AMOUNT, INTEREST_RATE);
		credit.setPayment(new BigDecimal("300"));

		Helper.assertEquals(new BigDecimal("160"), credit.getInterest(END_OF_FIRST_YEAR));
		Helper.assertEquals(CREDIT_AMOUNT, credit.amount(END_OF_FIRST_YEAR));

		Helper.assertEquals(new BigDecimal("159.5333333333333"), credit.getInterest(FIRST_DAY_OF_SECOND_YEAR));
		Helper.assertEquals(new BigDecimal("47860"), credit.amount(FIRST_DAY_OF_SECOND_YEAR));
	}

	@Test
	public void testMonthlyPayment2Month() throws Exception {
		Credit credit = new Credit(Day.of(2008, 10, 1), END_OF_THIRD_YEAR, CREDIT_AMOUNT, INTEREST_RATE);
		credit.setPayment(new BigDecimal("300"));

		Helper.assertEquals(new BigDecimal("319"), credit.getInterest(END_OF_FIRST_YEAR));
		Helper.assertEquals(new BigDecimal("47700"), credit.amount(END_OF_FIRST_YEAR));

		Helper.assertEquals(new BigDecimal("47719"), credit.amount(FIRST_DAY_OF_SECOND_YEAR));
	}

	@Test
	public void yearlyPaimentFirstYear() throws Exception {
		Credit credit = new Credit(Day.of(2008, 9, 1), END_OF_THIRD_YEAR, CREDIT_AMOUNT, INTEREST_RATE);
		credit.setPayment(new BigDecimal("5000"));
		credit.setPaymentInterval(PaymentInterval.YEARLY);

		Helper.assertEquals(CREDIT_AMOUNT, credit.amount(BEFORE_END_OF_FIRST_YEAR));
		// 48000 * 4% / 12 * 3 = 160 * 3 = 480
		Helper.assertEquals(new BigDecimal("480"), credit.getInterest(BEFORE_END_OF_FIRST_YEAR));

		Helper.assertEquals(CREDIT_AMOUNT, credit.amount(END_OF_FIRST_YEAR));
		Helper.assertEquals(new BigDecimal("480"), credit.getInterest(END_OF_FIRST_YEAR));

		Helper.assertEquals(new BigDecimal("48480"), credit.amount(FIRST_MONTH_OF_SECOND_YEAR));
		Helper.assertEquals(new BigDecimal("161.60"), credit.getInterest(FIRST_MONTH_OF_SECOND_YEAR));

		Day firstPayment = Day.of(2009, 9, 1);
		// 48000 + 480 - 5000
		Helper.assertEquals(new BigDecimal("43480"), credit.amount(firstPayment));
		// (9 * 161,60) + 1 * (43480 * 4% / 12)
		Helper.assertEquals(new BigDecimal("1599.33333333333333"), credit.getInterest(firstPayment));

		// (9 * 161,60) + 3 * (43480 * 4% / 12)
		Helper.assertEquals(new BigDecimal("1889.20"), credit.getInterest(END_OF_SECOND_YEAR));

		// 43480 + 1889.20
		Helper.assertEquals(new BigDecimal("45369.20"), credit.amount(FIRST_DAY_OF_THIRD_YEAR));
		Helper.assertEquals(new BigDecimal("151.2306666666667"), credit.getInterest(FIRST_DAY_OF_THIRD_YEAR));
	}

}
