package de.tomsplayground.peanuts.domain.option;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.client.Contract;
import com.ib.client.Execution;

import de.tomsplayground.peanuts.app.ib.IbConnection;
import de.tomsplayground.peanuts.app.ib.IbConnection.FullExec;
import de.tomsplayground.peanuts.domain.option.Option.Type;
import de.tomsplayground.peanuts.util.Day;

public class IbOptions {
	
	private final static Logger log = LoggerFactory.getLogger(IbOptions.class);

	private static final DateTimeFormatter TRADE_TIME_PATTERN = DateTimeFormatter.ofPattern("yyyMMdd HH:mm:ss z");

	public IbOptions() {
	}

	public static void main(String[] args) throws InterruptedException {
		IbOptions ibOptions = new IbOptions();
		IbConnection ibConnection = new IbConnection();
		ibConnection.start();
		while (! ibConnection.isConnected()) {
			Thread.sleep(100);
		}

		OptionsLog optionsLog = new OptionsLog();
		ibOptions.optionsFromIb(ibConnection.getTradeLog(), optionsLog);
		for (OptionsLog.LogEntry entry : optionsLog.getEntries()) {
			System.out.println(entry);
		}
		
		ibConnection.stop();
	}
	
	public List<Option> optionsFromIb(List<IbConnection.FullExec> executions, OptionsLog optionsLog) {
		List<Option> options = new ArrayList<>();
		executions.sort((a, b) -> a.getTrade().time().compareTo(b.getTrade().time()));
		List<FullExec> optionExecutions = executions.stream()
			.filter(x -> x.getContract().getSecType().equals("OPT"))
			.toList();
		for (FullExec fullExec : optionExecutions) {
			// Option
			Contract contract = fullExec.getContract();
			Type type = switch (contract.right()) {
				case Call -> Option.Type.Call;
				case Put -> Option.Type.Put;
				default -> null;
			};
			BigDecimal strike = new BigDecimal(contract.strike());
			LocalDate endDate = LocalDate.parse(contract.lastTradeDateOrContractMonth(), DateTimeFormatter.BASIC_ISO_DATE);
			Currency currency = Currency.getInstance(contract.currency());
			Option option = new Option(type, contract.symbol(), 0, "", strike, currency, Day.from(endDate));
			log.info("Option from tradelog: {}", option);
			options.add(option);
			
			// Execution
			Execution trade = fullExec.getTrade();
			BigDecimal quantity = trade.shares().value();
			int factor = switch(trade.side()) {
				case "BOT" -> 1;
				case "SLD" -> -1;
				default -> { log.error("Unkown trade side {}", trade.side()); yield 0; }
			};
			BigDecimal price = new BigDecimal(trade.price());
			BigDecimal commission = new BigDecimal(fullExec.getCommissionReport().commission());
			LocalDateTime dateTime = LocalDateTime.from(TRADE_TIME_PATTERN.parse(trade.time()));
			// TODO: fxRateToBase
			optionsLog.addEntry(quantity.intValue()*factor, option, price, commission, dateTime, BigDecimal.ONE, false, 
					trade.execId(), trade.orderRef());
		}
		return options;
	}
	
}
