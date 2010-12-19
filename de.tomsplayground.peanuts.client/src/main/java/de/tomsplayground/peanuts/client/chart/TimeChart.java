package de.tomsplayground.peanuts.client.chart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.Calendar;
import java.util.List;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYDrawableAnnotation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.Drawable;

import de.tomsplayground.peanuts.domain.statistics.Signal;
import de.tomsplayground.peanuts.domain.statistics.Signal.Type;

public class TimeChart {

	final private JFreeChart chart;
	final private TimeSeriesCollection series;

	final static private Drawable buyDrawable = new Drawable() {
		@Override
		public void draw(Graphics2D g, Rectangle2D area) {
			Rectangle rectangle = area.getBounds();
			g.setPaint(Color.GREEN);
	        g.setStroke(new BasicStroke(2));
			g.drawLine(rectangle.x, rectangle.y, rectangle.x, rectangle.y - 10);
		}
	};

	final static private Drawable sellDrawable = new Drawable() {
		@Override
		public void draw(Graphics2D g, Rectangle2D area) {
			Rectangle rectangle = area.getBounds();
			g.setPaint(Color.RED);
	        g.setStroke(new BasicStroke(2));
			g.drawLine(rectangle.x, rectangle.y, rectangle.x, rectangle.y + 10);
		}
	};

	public TimeChart(JFreeChart chart, TimeSeriesCollection series) {
		this.chart = chart;
		this.series = series;
	}
	
	public void setChartType(String type) {
		XYPlot plot = (XYPlot) chart.getPlot();
		DateAxis dateAxis = ((DateAxis)plot.getDomainAxis());
		Calendar from = Calendar.getInstance();
		Calendar to = Calendar.getInstance();
		if (type.equals("three years")) {
			from.add(Calendar.YEAR, -3);
			dateAxis.setRange(from.getTime(), to.getTime());
		} else if (type.equals("one year")) {
			from.add(Calendar.YEAR, -1);
			dateAxis.setRange(from.getTime(), to.getTime());
		} else if (type.equals("this year")) {
			from.set(Calendar.DAY_OF_MONTH, 1);
			from.set(Calendar.MONTH, 0);
			dateAxis.setRange(from.getTime(), to.getTime());
		} else if (type.equals("6 month")) {
			from.add(Calendar.MONTH, -6);
			dateAxis.setRange(from.getTime(), to.getTime());
		} else if (type.equals("1 month")) {
			from.add(Calendar.MONTH, -1);
			dateAxis.setRange(from.getTime(), to.getTime());
		} else {
			dateAxis.setAutoRange(true);					
		}
		adjustRangeAxis(plot, from, to);
	}

	public void addSignals(List<Signal> signals) {
		XYPlot plot = (XYPlot) chart.getPlot();
		for (Signal signal : signals) {
			long timeInMillis = signal.getDay().toCalendar().getTimeInMillis();
			double yValue = signal.getPrice().getClose().doubleValue();
			XYDrawableAnnotation annotation;
			if (signal.getType() == Type.SELL)
				annotation = new XYDrawableAnnotation(timeInMillis, yValue, 0, 0, sellDrawable);
			else
				annotation = new XYDrawableAnnotation(timeInMillis, yValue, 0, 0, buyDrawable);
			annotation.setToolTipText(signal.getPrice().getDay().toString());
			plot.addAnnotation(annotation);
		}
	}
	
	private void adjustRangeAxis(XYPlot plot, Calendar from, Calendar to) {
		if (plot.getDomainAxis().isAutoRange()) {
			plot.getRangeAxis().setAutoRange(true);
		} else {
			// Calculate min/max for y-axis
			double max = Double.MIN_VALUE;
			double min = Double.MAX_VALUE;
			for (int s = 0; s < series.getSeriesCount(); s++) {
				TimeSeries serie = series.getSeries(s);
				@SuppressWarnings("unchecked")
				Class<RegularTimePeriod> timePeriodClass = serie.getTimePeriodClass();
				if (! serie.isEmpty()) {
					int start = serie.getIndex(RegularTimePeriod.createInstance(timePeriodClass, from.getTime(), from.getTimeZone()));
					if (start < 0) {
						start = -start;
						if (start == serie.getItemCount())
							start = serie.getItemCount() - 1;
					}
					int end = serie.getIndex(RegularTimePeriod.createInstance(timePeriodClass, to.getTime(), to.getTimeZone()));
					if (end < 0) {
						end = - end;
					}
					if (end >= serie.getItemCount())
						end = serie.getItemCount() - 1;
					for (int i = start; i <= end; i++) {
						double v = serie.getValue(i).doubleValue();
						if (v < min)
							min = v;
						if (v > max)
							max = v;
					}
				}
			}
			plot.getRangeAxis().setRangeWithMargins(new Range(min, max));
		}
	}
}
