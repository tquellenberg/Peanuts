package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;

import de.tomsplayground.util.Day;

public interface IPrice {

	Day getDay();

	BigDecimal getValue();

	BigDecimal getOpen();

	BigDecimal getClose();

	BigDecimal getHigh();

	BigDecimal getLow();

}