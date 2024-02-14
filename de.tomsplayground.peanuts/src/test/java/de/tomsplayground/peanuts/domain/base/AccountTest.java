package de.tomsplayground.peanuts.domain.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.Before;
import org.junit.Test;

public class AccountTest {
	
	private Account account;

	@Before
	public void setUp() {
		account = new Account("name", Currency.getInstance("EUR"), BigDecimal.ZERO,
			Account.Type.BANK, "");
	}

	@Test
	public void testIbanNull() {
		assertNotNull(account.getIban());
		assertNotNull(account.getIbanReadable());
	}
	
	@Test
	public void testIbanSimple() {
		account.setIban("DE00 1234 5678 9012 3456 34");
		assertEquals("DE00123456789012345634", account.getIban());
		assertEquals("DE00 1234 5678 9012 3456 34", account.getIbanReadable());
	}
	
	@Test
	public void testIbanUpperCase() {
		account.setIban("de00 1234 5678 9012 3456 34");
		assertEquals("DE00123456789012345634", account.getIban());
	}
}
