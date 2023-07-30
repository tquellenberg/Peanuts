package de.tomsplayground.peanuts.domain.option;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tomsplayground.peanuts.util.PeanutsUtil;

public class OptionsLog {

	private final static Logger log = LoggerFactory.getLogger(OptionsLog.class);

	public static class LogEntry {
		private final String execId;
		private int quantity;
		private Option option;
		private BigDecimal pricePerOption;
		private BigDecimal commission;
		private LocalDateTime date;
		private BigDecimal fxRateToBase;
		private boolean assignment;

		public LogEntry(String execId, int quantity, Option option, BigDecimal pricePerOption, BigDecimal commission,
				LocalDateTime date, BigDecimal fxRateToBase, boolean assignment) {
			this.execId = execId;
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
			return pricePerOption.multiply(BigDecimal.valueOf(-quantity)).add(commission).setScale(2, RoundingMode.HALF_UP);
		}

		public BigDecimal getCostBaseCurrency() {
			return getCost().multiply(fxRateToBase, PeanutsUtil.MC).setScale(2, RoundingMode.HALF_UP);
		}
		public LocalDateTime getDate() {
			return date;
		}
		public int getQuantity() {
			return quantity;
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
		
		public Gain add(LogEntry e) {
			if (longTrade && e.quantity > 0) {
				trades.add(e);
				return null;
			}
			if (!longTrade && e.quantity < 0) {
				trades.add(e);
				return null;
			}
			int quantity = Math.abs(e.quantity);
			BigDecimal cost = BigDecimal.ZERO;
			for (int i = 0; i < quantity; i++) {
				cost = cost.add(getCostOfPosition(pop));
				pop++;
			}
			if (e.assignment) {
				return null;
			}
			return new Gain(e.date, cost.add(e.getCostBaseCurrency()), longTrade, e.option, -e.quantity);
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
					return logEntry.getCostBaseCurrency().divide(new BigDecimal(q), PeanutsUtil.MC);
				}
			}
			throw new IllegalArgumentException("");
		}
		
		public BigDecimal getCostBaseCurrency() {
			BigDecimal cost = BigDecimal.ZERO;
			for (LogEntry logEntry : trades) {
				cost = cost.add(logEntry.getCostBaseCurrency());
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
			LocalDateTime date, BigDecimal fxRateToBase, boolean assignment, String execId) {
		if (StringUtils.isNotBlank(execId)) {
			for (LogEntry logEntry : entries) {
				if (logEntry.execId.equals(execId)) {
					log.info("Opions trade already exists in log: '{}' {}", execId, option);
					return;
				}
			}
		}
		LogEntry logEntry = new LogEntry(execId, quantity, option, pricePerOption, commission, date, fxRateToBase, assignment);
		entries.add(logEntry);
		entries.sort((a, b) -> a.date.compareTo(b.date));
	}

	public record Gain(LocalDateTime d, BigDecimal gain, boolean longTrade, Option option, int quantity) {
	}

	public List<Gain> getGains() {
		return gains;
	}
	
	public void doit() {
		tradePerOption.clear();
		gains.clear();
		
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

	public List<LogEntry> getEntries() {
		return entries;
	}
	
	public Map<Option, TradesPerOption> getTradePerOption() {
		return tradePerOption;
	}

}
