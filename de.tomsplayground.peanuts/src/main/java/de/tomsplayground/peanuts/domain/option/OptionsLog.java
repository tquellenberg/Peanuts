package de.tomsplayground.peanuts.domain.option;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tomsplayground.peanuts.util.PeanutsUtil;

public class OptionsLog {

	private final static Logger log = LoggerFactory.getLogger(OptionsLog.class);

	public static class LogEntry {
		// Unique executions id
		private final String execId;
		// multiple executions can belong to ONE order (can be empty)
		private final String orderId;
		private int quantity;
		private Option option;
		private BigDecimal pricePerOption;
		private BigDecimal commission;
		private LocalDateTime date;
		private BigDecimal fxRateToBase;
		private boolean assignment;

		public LogEntry(String execId, String orderId, int quantity, Option option, BigDecimal pricePerOption, 
				BigDecimal commission, LocalDateTime date, BigDecimal fxRateToBase, boolean assignment) {
			this.execId = execId;
			this.orderId = orderId;
			this.quantity = quantity;
			this.option = option;
			this.pricePerOption = pricePerOption;
			this.commission = commission;
			this.date = date;
			this.fxRateToBase = fxRateToBase;
			this.assignment = assignment;
		}

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
		}

		public BigDecimal getCost() {
			return pricePerOption.multiply(BigDecimal.valueOf(-quantity)).add(getCommission()).setScale(2, RoundingMode.HALF_UP);
		}

		public BigDecimal getCostInBaseCurrency() {
			return getCost().multiply(fxRateToBase, PeanutsUtil.MC).setScale(2, RoundingMode.HALF_UP);
		}
		public LocalDateTime getDate() {
			return date;
		}
		public BigDecimal getCommission() {
			return commission.setScale(2, RoundingMode.HALF_UP);
		}
		public int getQuantity() {
			return quantity;
		}
		public String getOrderId() {
			return orderId;
		}
	}

	public static class TradesPerOption {
		private final List<LogEntry> trades = new ArrayList<>();
		private final Option option;
		private boolean longTrade;
		private int pop = 0;

		public TradesPerOption(LogEntry e) {
			trades.add(e);
			option = e.option;
			longTrade = e.quantity > 0;
		}

		public List<LogEntry> getTrades() {
			return trades;
		}
		
		private boolean isOpening(LogEntry e) {
			return (longTrade && e.quantity > 0) || (!longTrade && e.quantity < 0);
		}
		
		public Gain add(LogEntry e) {
			if (isOpening(e)) {
				trades.add(e);
				return null;
			}
			// Closing trade
			int quantity = Math.abs(e.quantity);
			BigDecimal openingCosts = BigDecimal.ZERO;
			for (int i = 0; i < quantity; i++) {
				openingCosts = openingCosts.add(getCostOfPosition(pop));
				pop++;
			}
			if (longTrade && e.assignment) {
				return null;
			}
			BigDecimal closingCosts = e.getCostInBaseCurrency();
			BigDecimal gainInBaseCurrency = openingCosts.add(closingCosts);
			return new Gain(e.date, openingCosts, closingCosts, gainInBaseCurrency, e.pricePerOption, e.getCommission(),
					longTrade, e.option, -e.quantity);
		}
		
		public boolean isEmpty() {
			int q = 0;
			for (LogEntry logEntry : trades) {
				q = q + Math.abs(logEntry.quantity);
			}
			return pop == q;
		}
		
		private BigDecimal getCostOfPosition(int pos) {
			for (LogEntry logEntry : trades) {
				int q = Math.abs(logEntry.quantity);
				if (pos >= q) {
					pos = pos - q;
				} else {
					return logEntry.getCostInBaseCurrency().divide(new BigDecimal(q), PeanutsUtil.MC);
				}
			}
			log.error("Problem in {} {}", option, trades);
			throw new IllegalArgumentException("");
		}
		
		public BigDecimal getCostBaseCurrency() {
			BigDecimal cost = BigDecimal.ZERO;
			for (LogEntry logEntry : trades) {
				cost = cost.add(logEntry.getCostInBaseCurrency());
			}
			return cost;
		}
		
		public Option getOption() {
			return option;
		}
		
		public int getQuantity() {
			return trades.stream().mapToInt(e -> e.quantity).sum();
		}
	}

	private final List<LogEntry> entries = new ArrayList<>();

	private final Map<Option, TradesPerOption> tradePerOption = new HashMap<>();
	
	private final List<Gain> gains = new ArrayList<>();

	public void addEntry(int quantity, Option option, BigDecimal pricePerOption, BigDecimal commission,
			LocalDateTime date, BigDecimal fxRateToBase, boolean assignment, String execId, String orderId) {
		if (StringUtils.isNotBlank(execId)) {
			for (LogEntry logEntry : entries) {
				if (logEntry.execId.equals(execId)) {
					log.error("Opions trade already exists in log: '{}' {}", execId, option);
					return;
				}
			}
		}
		LogEntry logEntry = new LogEntry(execId, orderId, quantity, option, pricePerOption, commission, 
				date, fxRateToBase, assignment);
		entries.add(logEntry);
	}

	public record Gain(LocalDateTime d, BigDecimal openingCosts, BigDecimal closingCosts,
			BigDecimal gain, BigDecimal pricePerOption, BigDecimal commission, 
			boolean longTrade, Option option, int quantity) {
	}

	public List<Gain> getGains() {
		return gains;
	}
	
	public void doit() {
		tradePerOption.clear();
		gains.clear();
		
		joinSplittedExecutions(entries);
		entries.sort((a, b) -> a.date.compareTo(b.date));
		
		for (LogEntry logEntry : entries) {
			Option option = logEntry.option;
			if (tradePerOption.containsKey(option)) {
				Gain gain = tradePerOption.get(option).add(logEntry);
				if (gain != null) {
					gains.add(gain);
				}
				if (tradePerOption.get(option).isEmpty()) {
					tradePerOption.remove(option);
				}
			} else {
				tradePerOption.put(option, new TradesPerOption(logEntry));
			}
		}
	}

	private void joinSplittedExecutions(List<LogEntry> entries) {
		Map<String, List<LogEntry>> collect = entries.stream()
			.filter(e -> StringUtils.isNotBlank(e.getOrderId()))
			.collect(Collectors.groupingBy(LogEntry::getOrderId));
		
		List<LogEntry> resultList = new ArrayList<>();
		for (List<LogEntry> logEntries : collect.values()) {
			if (logEntries.size() > 1) {
				LogEntry firstLogEntry = logEntries.get(0);
				entries.remove(firstLogEntry);
				LogEntry joinedLogEntry = new LogEntry("Joined: "+firstLogEntry.execId, firstLogEntry.orderId, 
						firstLogEntry.quantity, firstLogEntry.option, BigDecimal.ZERO, firstLogEntry.commission, 
						firstLogEntry.date, firstLogEntry.fxRateToBase, firstLogEntry.assignment);
				BigDecimal mixedPrice = firstLogEntry.pricePerOption.multiply(new BigDecimal(firstLogEntry.quantity));
				for (int i = 1; i < logEntries.size(); i++) {
					LogEntry additinalLogEntry = logEntries.get(i);
					if (! firstLogEntry.option.equals(additinalLogEntry.option)) {
						log.error("ERROR {} {} {}", firstLogEntry.orderId, firstLogEntry.option, additinalLogEntry.option);
					}
					entries.remove(additinalLogEntry);
					mixedPrice = mixedPrice.add(additinalLogEntry.pricePerOption.multiply(new BigDecimal(additinalLogEntry.quantity)));
					joinedLogEntry.quantity = joinedLogEntry.quantity + additinalLogEntry.quantity;
					joinedLogEntry.commission = joinedLogEntry.commission.add(additinalLogEntry.commission);
				}
				joinedLogEntry.pricePerOption = mixedPrice.divide(new BigDecimal(joinedLogEntry.quantity), PeanutsUtil.MC);
				entries.add(joinedLogEntry);
				log.info(" Joined entry: {}", joinedLogEntry);
			} else {
				resultList.add(logEntries.get(0));
				log.info("Single entry {}", logEntries.get(0).getOrderId());
			}
		}
	}

	public List<LogEntry> getEntries() {
		return entries;
	}
	
	public Map<Option, TradesPerOption> getTradePerOption() {
		return tradePerOption;
	}

}
