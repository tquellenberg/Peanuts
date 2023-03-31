package de.tomsplayground.peanuts.client.chart;

import java.awt.Color;
import java.awt.Paint;
import java.util.Map;

import org.jfree.chart.plot.DefaultDrawingSupplier;

/*
 * Color-Schema:
 * http://www.wellstyled.com/tools/colorscheme2/index-en.html?tetrad;50;0;225;-1;-1;1;-0.7;0.25;1;0.5;1;-1;-1;1;-0.7;0.25;1;0.5;1;-1;-1;1;-0.7;0.25;1;0.5;1;-1;-1;1;-0.7;0.25;1;0.5;1;0
 */
public class PeanutsDrawingSupplier extends DefaultDrawingSupplier {

	private static final long serialVersionUID = 8571702014576886770L;

	public final static Paint BACKGROUND_PAINT = new Color(0xFFF2BF);
	public final static Paint GRIDLINE_PAINT = new Color(0x80C9FF);

	final static Paint[] PEANUTS_DEFAULT_PAINT_SEQUENCE  = new Paint[] {
		new Color(0x0066B3),
		new Color(0xFF8000),
		new Color(0xFFCC00),
		new Color(0x330099),

		new Color(0x00487D),
		new Color(0xB35A00),
		new Color(0xB38F00),
		new Color(0x24006B),

		new Color(0x80C9FF),
		new Color(0xFFC080),
		new Color(0xFFE680),
		new Color(0xAA80FF)
	};
	
	public static final Map<String, Color> defaultColors = Map.of(
			"MA20", new Color(0x89CFF0), 
			"MA100", new Color(0x6495ED) , 
			"MA200", new Color(0x00008B));

	public PeanutsDrawingSupplier() {
		super(PEANUTS_DEFAULT_PAINT_SEQUENCE,
			DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
			DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
			DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
			DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE);
	}

	public static Paint getColor(String name) {
		return defaultColors.get(name);
	}
}
