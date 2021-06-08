package de.tomsplayground.peanuts.client.dividend;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import de.tomsplayground.peanuts.client.chart.PeanutsDrawingSupplier;
import de.tomsplayground.peanuts.domain.dividend.DividendMonth;
import de.tomsplayground.peanuts.domain.dividend.DividendStats;
import de.tomsplayground.peanuts.util.Day;

public class DividendChart {

	private static final float BASIC_WIDTH = 2.5f;
	private static final float BOLD_WIDTH = 4.0f;

	private static final int ALPHA = 0xB4;

	private static final Color BASE_COLOR_1 = new Color(0x330099 + (ALPHA << 24), true);
	private static final Color BASE_COLOR_2 = new Color(0xFF8000 + (ALPHA << 24), true);

	private static final BasicStroke BASIC_STROKE = new BasicStroke(BASIC_WIDTH);
	private static final BasicStroke BOLD_STROKE = new BasicStroke(BOLD_WIDTH);

	private static final BasicStroke BASIC_DASH = new BasicStroke(BASIC_WIDTH,
        BasicStroke.CAP_BUTT,
        BasicStroke.JOIN_MITER,
        10.0f, new float[]{5.0f}, 0.0f);
	private static final BasicStroke BOLD_DASH = new BasicStroke(BOLD_WIDTH,
        BasicStroke.CAP_BUTT,
        BasicStroke.JOIN_MITER,
        10.0f, new float[]{5.0f}, 0.0f);

	private final DividendStats dividendStats;

	private int selectedYear = -1;
	private Integer startYear = null;

	private StandardXYItemRenderer renderer;

	public DividendChart(DividendStats dividendStats) {
		this.dividendStats = dividendStats;
	}

	public void selectYear(int year) {
		// Deselect old serie
		deselectYear();
		// Select new serie
		int currentYear = Day.today().year;
		int newSeriesNumber = year-startYear;
		if (year > currentYear) {
			newSeriesNumber++;
		}
		selectedYear = year;
		renderer.setSeriesStroke(newSeriesNumber, BOLD_STROKE);
		if (selectedYear == currentYear) {
			renderer.setSeriesStroke(newSeriesNumber+1, BOLD_DASH);
		}
	}

	public void deselectYear() {
		int currentYear = Day.today().year;
		if (selectedYear != -1) {
			// Deselect old serie
			int oldSeriesNumber = selectedYear-startYear;
			if (selectedYear > currentYear) {
				oldSeriesNumber++;
			}
			renderer.setSeriesStroke(oldSeriesNumber, BASIC_STROKE);
			if (selectedYear == currentYear) {
				renderer.setSeriesStroke(oldSeriesNumber+1, BASIC_DASH);
			}
			selectedYear = -1;
		}
	}

	public JFreeChart createChart() {
		renderer = new StandardXYItemRenderer();
		XYDataset dataset = createTotalDataset(renderer);
		JFreeChart chart = ChartFactory.createXYLineChart(
			"Dividends", // title
			"Month", // x-axis label
			"Sum", // y-axis label
			dataset,
			PlotOrientation.VERTICAL,
			true, // create legend?
			true, // generate tooltips?
			false // generate URLs?
		);
		chart.setBackgroundPaint(java.awt.Color.white);

		XYPlot plot = (XYPlot) chart.getPlot();
		plot.setDomainGridlinePaint(PeanutsDrawingSupplier.GRIDLINE_PAINT);
		plot.setRangeGridlinePaint(PeanutsDrawingSupplier.GRIDLINE_PAINT);
		plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);
		plot.setDrawingSupplier(new PeanutsDrawingSupplier());
		plot.setRenderer(renderer);

		plot.getDomainAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());

		return chart;
	}

	private XYDataset createTotalDataset(StandardXYItemRenderer renderer) {
		YearMonth currentMonth = Day.today().toYearMonth();
		int year = 0;
		XYSeries timeSeries = null;
		List<XYSeries> series = new ArrayList<>();
		boolean future = false;
		for (DividendMonth dividendMonth : dividendStats.getDividendMonths()) {
			if (dividendMonth.getMonth().getYear() != year) {
				// Start next year
				year = dividendMonth.getMonth().getYear();
				if (startYear == null) {
					startYear = year;
				}
				timeSeries = newSeries(renderer, year, series, future);
			}
			if (! future && dividendMonth.getMonth().compareTo(currentMonth) >= 0) {
				timeSeries.add(Integer.valueOf(dividendMonth.getMonth().getMonthValue()), dividendMonth.getYearlyAmount());
				// Switch to future
				future = true;
				timeSeries = newSeries(renderer, year, series, future);
			}
			if (future) {
				timeSeries.add(Integer.valueOf(dividendMonth.getMonth().getMonthValue()), dividendMonth.getFutureYearlyAmount());
			} else {
				timeSeries.add(Integer.valueOf(dividendMonth.getMonth().getMonthValue()), dividendMonth.getYearlyAmount());
			}
		}

		XYSeriesCollection dataset = new XYSeriesCollection();
		for (XYSeries timeSeries2 : series) {
			dataset.addSeries(timeSeries2);
		}
		return dataset;
	}

	private XYSeries newSeries(StandardXYItemRenderer renderer, int year, List<XYSeries> series, boolean future) {
		XYSeries timeSeries = new XYSeries(getSeriesName(year, future));
		if (future) {
			renderer.setSeriesStroke(series.size(), BASIC_DASH);
		} else {
			renderer.setSeriesStroke(series.size(), BASIC_STROKE);
		}
		renderer.setSeriesPaint(series.size(), getColor(year));
		series.add(timeSeries);
		return timeSeries;
	}

	private Paint getColor(int year) {
		int delta = Day.today().year -year;
		// Current year
		if (delta == 0) {
			return Color.RED;
		}
		// Future year
		if (delta < 0) {
			return Color.LIGHT_GRAY;
		}
		// Past year
		Color color = BASE_COLOR_1;
		if (delta > 5) {
			color = BASE_COLOR_2;
			delta = delta - 5;
		}
		for (int i = 1; i < delta; i++ ) {
			color = brighter(color);
		}
		return color;
	}

	private Color brighter(Color c) {
		double f = 0.30;
		int r = Math.min(c.getRed() + Math.max((int)((255-c.getRed()) * f), 1), 255);
		int g = Math.min(c.getGreen() + Math.max((int)((255-c.getGreen()) * f), 1), 255);
		int b = Math.min(c.getBlue() + Math.max((int)((255-c.getBlue()) * f), 1), 255);
		return new Color(r, g, b, c.getAlpha());
	}

	private String getSeriesName(int year, boolean future) {
		return ""+ year + ((future)?" future":"");
	}

}
