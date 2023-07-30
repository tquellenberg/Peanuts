package de.tomsplayground.peanuts.app.ib;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.client.CommissionReport;
import com.ib.client.Contract;
import com.ib.client.Decimal;
import com.ib.client.Execution;
import com.ib.client.ExecutionFilter;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.Types;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.DurationUnit;
import com.ib.client.Types.SecType;
import com.ib.client.Types.WhatToShow;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.ICompletedOrdersHandler;
import com.ib.controller.ApiController.IConnectionHandler;
import com.ib.controller.ApiController.IHistoricalDataHandler;
import com.ib.controller.ApiController.IPositionHandler;
import com.ib.controller.ApiController.ITradeReportHandler;
import com.ib.controller.Bar;
import com.ib.controller.Formats;

import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.util.Day;

public class IbConnection implements IConnectionHandler {

	private final static Logger log = LoggerFactory.getLogger(IbConnection.class);

	// e.g. "20230616 15:00:00 Europe/Berlin"
	private final static DateTimeFormatter ibDateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss VV");

	private static final DateTimeFormatter TRADE_DATE_PATTERN = DateTimeFormatter.ofPattern("yyyMMdd");

	private ApiController apiController;

	private AtomicBoolean connected = new AtomicBoolean(false);

	public static class FullExec {
		private Contract contract;
		private Execution trade;
		private CommissionReport commissionReport;

		FullExec(Contract contract, Execution trade) {
			this.contract = contract;
			this.trade = trade;
		}
		public void setCommissionReport(CommissionReport commissionReport) {
			this.commissionReport = commissionReport;
		}
		public CommissionReport getCommissionReport() {
			return commissionReport;
		}
		public Contract getContract() {
			return contract;
		}
		public Execution getTrade() {
			return trade;
		}
	}

	public static void main(String[] args) throws InterruptedException {
		IbConnection ibConnection = new IbConnection();
		ibConnection.start();
		while (! ibConnection.isConnected()) {
			Thread.sleep(100);
		}

//		ibConnection.getIvrData("BAX");
//		ibConnection.getOrders();
//		ibConnection.getPositions();
//		ibConnection.getTradeLog();
		Price lastPrice = ibConnection.getLastPrice("SPX", "CBOE", 416904);
		System.out.println(lastPrice);

		Thread.sleep(1000*10*1);
		ibConnection.stop();
	}

	public Price getLastPrice(String symbol, String exchange, int conId) throws InterruptedException {
		if (StringUtils.isBlank(exchange) || exchange.equals("NASDAQ") || exchange.equals("NYSE")) {
			exchange = "SMART";
		}
		log.info("getLastPrice: {} @{} ({})", symbol, exchange, conId);

		CountDownLatch conditionLatch = new CountDownLatch(1);
		
		AtomicReference<Price> lastPrice = new AtomicReference<>();;

		Contract contract = new Contract();
		if (StringUtils.isNotBlank(symbol)) {
			contract.symbol(symbol);
		}
		if (conId != 0) {
			contract.conid(conId);
		}
		contract.exchange(exchange);
		controller().reqHistoricalData(contract, "", 3, DurationUnit.DAY, BarSize._4_hours, 
				WhatToShow.ADJUSTED_LAST, false, false, new IHistoricalDataHandler() {
			@Override
			public void historicalData(Bar bar) {
				LocalDateTime dateTime = LocalDateTime.parse(bar.timeStr(), ibDateTimeFormatter);
				BigDecimal p = new BigDecimal(bar.close());
				lastPrice.set(new Price(Day.from(dateTime.toLocalDate()), p));
			}

			@Override
			public void historicalDataEnd() {
				conditionLatch.countDown();
			}
		});
		
		conditionLatch.await(5, TimeUnit.SECONDS);
		
		return lastPrice.get();
	}
	
	public List<FullExec> getTradeLog() {
		Map<String, FullExec> map = new HashMap<>();

		CountDownLatch conditionLatch = new CountDownLatch(1);
		ExecutionFilter executionFilter = new ExecutionFilter();
		executionFilter.time(TRADE_DATE_PATTERN.format(LocalDate.now().minusDays(7)) + "-00:00:00"); // UTC
		controller().reqExecutions(executionFilter, new ITradeReportHandler() {
			@Override
			public void tradeReport(String tradeKey, Contract contract, Execution trade) {
				FullExec full = map.get(tradeKey);
				if (full != null) {
					full.trade = trade;
				} else {
					full = new FullExec(contract, trade);
					map.put(tradeKey, full);
				}
			}

			@Override
			public void commissionReport(String tradeKey, CommissionReport commissionReport) {
				FullExec full = map.get(tradeKey);
				if (full != null) {
					full.setCommissionReport(commissionReport);
				}
			}

			@Override
			public void tradeReportEnd() {
				conditionLatch.countDown();
				log.info("Ende");
			}
		});
		
		try {
			conditionLatch.await(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			log.info("", e);
		}
		log.info("getTradeLog {} entries.", map.size());
		return new ArrayList<>(map.values());
	}
	
	public void getPositions() {
		log.info("Los");
		controller().reqPositions(new IPositionHandler() {
			
			@Override
			public void position(String account, Contract contract, Decimal pos, double avgCost) {
				log.info("position: "+ contract+ " "+pos);
			}
			@Override
			public void positionEnd() {
				log.info("Ende");
			}
		});
	}
	
	public void getOrders() {
		log.info("Los");
		controller().reqCompletedOrders(new ICompletedOrdersHandler() {
			@Override
			public void completedOrder(Contract contract, Order order, OrderState orderState) {
				log.info("Order: "+ order+ " "+orderState);
			}

			@Override
			public void completedOrdersEnd() {
				log.info("Ende");
			}
		});
	}
	
	public IVR getIvrData(String symbol) {
		Contract contract = new Contract();
		contract.symbol(symbol);
		contract.secType(SecType.STK);
		contract.exchange("SMART");
		contract.primaryExch("ISLAND");
		contract.currency("USD");
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
		String endDateTime = simpleDateFormat.format(new Date())+" 23:59:00";
		Types.WhatToShow whatToShow = Types.WhatToShow.OPTION_IMPLIED_VOLATILITY;
		boolean useRth = false;
		Types.DurationUnit durationString = Types.DurationUnit.DAY;
		BarSize barSizeSetting = BarSize._1_day;
		int duration = 360;
		boolean keepUpToDate = false;

		IVR ivr = new IVR(symbol);
		controller().reqHistoricalData(contract, endDateTime, duration, durationString, barSizeSetting, whatToShow,
			useRth, keepUpToDate, ivr);

		return ivr;
	}

	public void stop() {
		log.info("Stopping");
		controller().disconnect();
	}

	public void start() {
		log.info("Starting");
		controller().connect("127.0.0.1", 7496, 0, null);
	}

	@Override
	public void accountList(List<String> arg0) {
		log.info("accountList: "+arg0);
	}

	@Override
	public void connected() {
		show("connected");
		controller().reqCurrentTime(time -> show( "Server date/time is " + Formats.fmtDate(time * 1000) ));
		controller().reqBulletins( true, (msgId, newsType, message, exchange) -> {
            String str = String.format( "Received bulletin:  type=%s  exchange=%s", newsType.name(), exchange);
            show(str);
            show(message);
        });
		connected.set(true);
	}

	@Override
	public void disconnected() {
		connected.set(false);
	}

	public ApiController controller() {
		if (apiController == null) {
			apiController = new ApiController(this, this::inLog, this::outLog);
		}
		return apiController;
	}

	@Override
	public void error(Exception e) {
		log.error("", e);
	}

	@Override
	public void message(int id, int errorCode, String errorMsg, String advancedOrderRejectJson) {
		log.info("Id:" + id + " Code:" + errorCode + " Message:" + errorMsg);
		if (advancedOrderRejectJson != null) {
			log.info(advancedOrderRejectJson);
		}
	}

	@Override
	public void show(String message) {
		log.info(message);
	}

	public void inLog(String message) {
		log.debug("IN : "+message);
	}

	public void outLog(String message) {
		log.debug("OUT: "+message);
	}

	public boolean isConnected() {
		return connected.get();
	}

}
