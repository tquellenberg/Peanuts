package de.tomsplayground.peanuts.app.ib;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.client.Contract;
import com.ib.client.Types;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.SecType;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IConnectionHandler;
import com.ib.controller.Formats;

public class IbConnection implements IConnectionHandler {

	private final static Logger log = LoggerFactory.getLogger(IbConnection.class);

	private ApiController apiController;

	private boolean connected = false;

	public static void main(String[] args) throws InterruptedException {
		IbConnection ibConnection = new IbConnection();
		ibConnection.start();

		ibConnection.getData("BAX");

		Thread.sleep(1000*10*1);
		ibConnection.stop();
	}

	public IVR getData(String symbol) {
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
		connected = true;
	}

	@Override
	public void disconnected() {
		show("disconnected");
		connected = false;
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
	public void message(int id, int errorCode, String errorMsg) {
		log.info("Id:" + id + " Code:" + errorCode + " Message:" + errorMsg);
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
		return connected;
	}

}
