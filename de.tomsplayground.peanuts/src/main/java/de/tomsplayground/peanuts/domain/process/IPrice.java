package de.tomsplayground.peanuts.domain.process;

import java.math.BigDecimal;

sealed public interface IPrice extends ITimedElement permits Price {

	BigDecimal getValue();

}