package de.tomsplayground.peanuts.client.chart;

import java.util.Calendar;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.google.common.collect.ImmutableList;

public class TimeChart {

	public enum RANGE {
		ALL("all"),
		TEN_YEARS("ten years"),
		SEVEN_YEARS("seven years"),
		FIVE_YEARS("five years"),
		THREE_YEARS("three years"),
		TWO_YEARS("two years"),
		ONE_YEARS("one year"),
		THIS_YEARS("this year"),
		SIX_MONTHS("6 months"),
		ONE_MONTHS("one month");

		private String name;

		RANGE(String text) {
			this.name = text;
		}
		public String getName() {
			return name;
		}
		public static RANGE fromName(String name) {
			for (RANGE r : RANGE.values()) {
				if (name.equals("1 month")) {
					return ONE_MONTHS;
				}
				if (name.equals("6 month")) {
					return SIX_MONTHS;
				}
				if (r.name.equals(name)) {
					return r;
				}
			}
			throw new IllegalArgumentException("Name: " +name);
		}
	};

	final private JFreeChart chart;
	final private TimeSeriesCollection series;
	private RANGE type;

	public TimeChart(JFreeChart chart, TimeSeriesCollection series) {
		this.chart = chart;
		this.series = series;
	}

	public void setChartType(String type) {
		this.type = RANGE.fromName(type);
		XYPlot plot = getPlot();
		DateAxis dateAxis = ((DateAxis)plot.getDomainAxis());
		Calendar to = Calendar.getInstance();
		to.add(Calendar.DAY_OF_MONTH, 14);
		Calendar from = getFromDate();
		if (from != null) {
			dateAxis.setRange(from.getTime(), to.getTime());
		} else {
			dateAxis.setAutoRange(true);
		}
		adjustRangeAxis(plot, from, to);
	}

	public Calendar getFromDate() {
		Calendar from = Calendar.getInstance();
		switch(type) {
			case TEN_YEARS: from.add(Calendar.YEAR, -10);
						break;
			case SEVEN_YEARS: from.add(Calendar.YEAR, -7);
						break;
			case FIVE_YEARS: from.add(Calendar.YEAR, -5);
						break;
			case THREE_YEARS: from.add(Calendar.YEAR, -3);
						break;
			case TWO_YEARS: from.add(Calendar.YEAR, -2);
						break;
			case ONE_YEARS: from.add(Calendar.YEAR, -1);
						break;
			case THIS_YEARS: from.set(Calendar.DAY_OF_MONTH, 1);
							 from.set(Calendar.MONTH, 0);
						break;
			case SIX_MONTHS: from.add(Calendar.MONTH, -6);
						break;
			case ONE_MONTHS: from.add(Calendar.MONTH, -1);
						break;
			default: from = null;
		}
		return from;
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
						start = -start - 1;
						if (start == serie.getItemCount()) {
							start = serie.getItemCount() - 1;
						}
					}
					int end = serie.getIndex(RegularTimePeriod.createInstance(timePeriodClass, to.getTime(), to.getTimeZone()));
					if (end < 0) {
						end = - end - 1;
					}
					if (end >= serie.getItemCount()) {
						end = serie.getItemCount() - 1;
					}
					for (int i = start; i <= end; i++) {
						double v = serie.getValue(i).doubleValue();
						if (v < min) {
							min = v;
						}
						if (v > max) {
							max = v;
						}
					}
					break;
				}
			}
			if (min < max) {
				plot.getRangeAxis().setRangeWithMargins(new Range(min, max));
			}
		}
	}

	public void removeAnnotations(ImmutableList<XYAnnotation> signalAnnotations) {
		XYPlot plot = getPlot();
		for (XYAnnotation xyAnnotation : signalAnnotations) {
			plot.removeAnnotation(xyAnnotation);
		}
	}

	public XYPlot getPlot() {
		if (chart.getPlot() instanceof CombinedDomainXYPlot) {
			return (XYPlot) ((CombinedDomainXYPlot)chart.getPlot()).getSubplots().get(0);
		} else {
			return chart.getXYPlot();
		}
	}
}
