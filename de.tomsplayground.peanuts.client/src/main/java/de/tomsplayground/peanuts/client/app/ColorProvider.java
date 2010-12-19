package de.tomsplayground.peanuts.client.app;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;

public class ColorProvider {

	public static final int LIST_EVEN = 0;
	public static final int LIST_ODD = 1;
	
	private Color colors[];

	public ColorProvider(Device device) {
		colors = new Color[2];
		colors[LIST_EVEN] = new Color(device, 0xBF, 0xE4, 0xFF);
		colors[LIST_ODD] = new Color(device, 0xFF, 0xF2, 0xBF);
	}
	
	public Color getColor(int number) {
		return colors[number];
	}
	
	public void dispose() {
		for (Color color : colors) {
			color.dispose();
		}
	}
}
