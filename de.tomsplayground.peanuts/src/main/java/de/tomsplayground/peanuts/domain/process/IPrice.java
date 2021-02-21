package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;

public interface IPrice extends ITimedElement {

	BigDecimal getValue();

}