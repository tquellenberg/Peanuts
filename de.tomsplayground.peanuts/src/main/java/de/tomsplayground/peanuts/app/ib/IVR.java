package de.tomsplayground.peanuts.app.ib;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.controller.ApiController;
import com.ib.controller.Bar;

public class IVR implements ApiController.IHistoricalDataHandler {

	private final static Logger log = LoggerFactory.getLogger(IVR.class);

	private final String symbol;

	private double low;
	private double high;

	private double current;

	private final CompletableFuture<Double> completableFuture = new CompletableFuture<>();
	private Double rank;

	public IVR(String symbol) {
		this.symbol = symbol;
		low = 1000;
		high = 0;
		current = 0;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setCurrent(double current) {
		this.current = current;
	}

	@Override
	public void historicalData(Bar bar) {
		if ((System.currentTimeMillis() - bar.time()) < 1000*60*60*24) {
			log.info("BAR: "+bar);
		}
		update(bar.close());
	}

	@Override
	public void historicalDataEnd() {
		log.info("IVR: min: " + low + " max: " + high + " current: "+current+ " rank: "+rank());
		completableFuture.complete(rank());
	}

	public double getRank() {
		if (rank == null) {
			try {
				rank = completableFuture.get(60, TimeUnit.SECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				log.error("", e);
				rank = Double.NaN;
			}
		}
		return rank;
	}

	public void update(double close) {
		this.current = close;
		if (close < low) {
			low = close;
		}
		if (close > high) {
			high = close;
		}
	}

	public double getHigh() {
		return high;
	}

	public double getLow() {
		return low;
	}

	public double rank() {
		return (current - low) / (high - low);
	}

}
