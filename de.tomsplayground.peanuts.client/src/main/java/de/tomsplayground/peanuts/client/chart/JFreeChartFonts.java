package de.tomsplayground.peanuts.client.chart;

import java.awt.Font;

import org.apache.commons.lang3.SystemUtils;

public class JFreeChartFonts {

	private static final Font TickLabelFont;
	
	static {
		if (SystemUtils.IS_OS_WINDOWS) {
			TickLabelFont = new Font(Font.SANS_SERIF, Font.PLAIN, 8);
		} else {
			TickLabelFont = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
		}
	}
	
	public static Font getTickLabelFont() {
		return TickLabelFont;
	}
	
}
