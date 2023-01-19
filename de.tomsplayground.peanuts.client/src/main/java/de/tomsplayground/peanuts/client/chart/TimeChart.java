package de.tomsplayground.peanuts.client.chart;

import java.util.Locale;
import java.util.TimeZone;

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

import de.tomsplayground.peanuts.util.Day;

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

	public void setChartType(String type, Day to) {
		this.type = RANGE.fromName(type);
		XYPlot plot = getPlot();
		DateAxis dateAxis = ((DateAxis)plot.getDomainAxis());

		to = to.addDays(rightOffset());

		Day from = getFromDate();
		if (from != null) {
			dateAxis.setRange(from.toDate(), to.toDate());
		} else {
			dateAxis.setAutoRange(true);
		}
		adjustRangeAxis(plot, from, to);
	}

	private int rightOffset() {
		switch(type) {
			case ALL:
			case TEN_YEARS:
			case SEVEN_YEARS:
				return 14;
			case FIVE_YEARS:
			case THREE_YEARS:
			case TWO_YEARS:
			case ONE_YEARS:
				return 7;
			case THIS_YEARS:
			case SIX_MONTHS:
			case ONE_MONTHS:
			default:
				return 2;
		}
	}

	public Day getFromDate() {
		Day from = switch(type) {
			case TEN_YEARS -> Day.today().addYear(-10);
			case SEVEN_YEARS -> Day.today().addYear(-7);
			case FIVE_YEARS -> Day.today().addYear(-5);
			case THREE_YEARS -> Day.today().addYear(-3);
			case TWO_YEARS -> Day.today().addYear(-2);
			case ONE_YEARS -> Day.today().addYear(-1);
			case THIS_YEARS -> Day.firstDayOfYear(Day.today().year);
			case SIX_MONTHS -> Day.today().addMonth(-6);
			case ONE_MONTHS -> Day.today().addMonth(-1);
			case ALL -> null;
		};
		return from;
	}

	private void adjustRangeAxis(XYPlot plot, Day from, Day to) {
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
					int start = serie.getIndex(RegularTimePeriod.createInstance(timePeriodClass, from.toDate(), TimeZone.getDefault(), Locale.getDefault()));
					if (start < 0) {
						start = -start - 1;
						if (start == serie.getItemCount()) {
							start = serie.getItemCount() - 1;
						}
					}
					int end = serie.getIndex(RegularTimePeriod.createInstance(timePeriodClass, to.toDate(), TimeZone.getDefault(), Locale.getDefault()));
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
		if (chart.getPlot() instanceof CombinedDomainXYPlot combinedPlot) {
			return (XYPlot) combinedPlot.getSubplots().get(0);
		} else {
			return chart.getXYPlot();
		}
	}
}
