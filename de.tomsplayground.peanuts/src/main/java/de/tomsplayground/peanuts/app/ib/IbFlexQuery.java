package de.tomsplayground.peanuts.app.ib;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.List;
import java.util.function.Consumer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.tomsplayground.peanuts.domain.option.Option;
import de.tomsplayground.peanuts.domain.option.OptionsLog;
import de.tomsplayground.peanuts.domain.option.OptionsLog.Gain;
import de.tomsplayground.peanuts.util.Day;
import de.tomsplayground.peanuts.util.PeanutsUtil;

public class IbFlexQuery {

	private static final DateTimeFormatter ibDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final DateTimeFormatter ibDateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd;HHmmss");

	private final OptionsLog optionsLog = new OptionsLog();

	public static void main(String[] args) {
		String filename = "/Users/quelle/Documents/Geld/InteractiveBrokers/FlexQuery_2023.xml";
		new IbFlexQuery().readOptionsFromXML(filename);
	}

	public OptionsLog readOptionsFromXML(String filename) {
		Document document;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			document = builder.parse(new File(filename));
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
			return null;
		}
		document.getDocumentElement().normalize();

		readTags(document, "Trade", trade);

//		List<LogEntry> entries = optionsLog.getEntries();
//		for (LogEntry logEntry : entries) {
//			System.out.println(logEntry + " => " + logEntry.getCostBaseCurrency());
//		}
		optionsLog.doit();
		
		List<Gain> gains = optionsLog.getGains();
		BigDecimal losses = BigDecimal.ZERO;
		BigDecimal winnes = BigDecimal.ZERO;
		for (Gain gain : gains) {
			if (gain.longTrade()) {
				System.out.println(gain);
				if (gain.gain().signum() == 1) {
					winnes = winnes.add(gain.gain());
				} else {
					losses = losses.add(gain.gain());
				}
			}
		}
		System.out.println("Sum of losses: "+ PeanutsUtil.formatCurrency(losses, null));
		System.out.println("Sum of winnes: "+ PeanutsUtil.formatCurrency(winnes, null));
		System.out.println("Saldo: "+ PeanutsUtil.formatCurrency(winnes.add(losses), null));

//		readTags(document, "CashTransaction");
//		readTags(document, "ConversionRate");
		
		return optionsLog;
	}

	private Consumer<Element> trade = e -> {
		// symbol, description

		String levelOfDetail = e.getAttribute("levelOfDetail");
		switch (levelOfDetail) {
		case "EXECUTION":
			break;
		default:
			System.err.println("Unknown levelOfDetail: " + levelOfDetail);
		}

		String currency = e.getAttribute("currency");
		switch (currency) {
		case "USD":
			break;
		case "GBP":
			break;
		default:
			System.err.println("Unknown currency: " + currency);
		}
		String ibCommissionCurrency = e.getAttribute("ibCommissionCurrency");
		switch (ibCommissionCurrency) {
		case "USD":
			break;
		case "GBP":
			break;
		case "EUR":
			break;
		default:
			System.err.println("Unknown ibCommissionCurrency: " + ibCommissionCurrency);
		}

		String dateTime = e.getAttribute("dateTime");
		LocalDateTime tradeDateTime = LocalDateTime.parse(dateTime, ibDateTimeFormatter);
		BigDecimal quantity = new BigDecimal(e.getAttribute("quantity"));
		BigDecimal tradePrice = new BigDecimal(e.getAttribute("tradePrice"));
		BigDecimal ibCommission = new BigDecimal(e.getAttribute("ibCommission"));
		BigDecimal fxRateToBase = new BigDecimal(e.getAttribute("fxRateToBase"));
		String execId = e.getAttribute("ibExecID");

		String buySell = e.getAttribute("buySell");
		switch (buySell) {
		case "BUY":
			break;
		case "SELL":
			break;
		default:
			System.err.println("Unknown buySell: " + buySell);
		}

		String assetCategory = e.getAttribute("assetCategory");
		switch (assetCategory) {
		case "STK":
			readStockTrade(e);
			break;
		case "OPT":
			Option option = readOptionTrade(e);
			boolean assignment = StringUtils.equals("A", e.getAttribute("notes"));
			if (StringUtils.equals("Ex", e.getAttribute("notes"))) {
				tradePrice = new BigDecimal(e.getAttribute("mtmPnl")).divide(quantity, PeanutsUtil.MC);
			} else {
				tradePrice = tradePrice.multiply(new BigDecimal("100"));
			}
			optionsLog.addEntry(quantity.intValue(), option, tradePrice, ibCommission,
					tradeDateTime, fxRateToBase, assignment, execId);
			break;
		case "CASH":
			readCashTrade(e);
			break;
		default:
			System.err.println("Unknown assetCategory: " + assetCategory);
		}
	};

	private void readTags(Document document, String tagName, Consumer<Element> doit) {
		NodeList tagList = document.getElementsByTagName(tagName);
		for (int i = 0; i < tagList.getLength(); i++) {
			Node nNode = tagList.item(i);
			doit.accept((Element) nNode);
		}
	}

	private void readStockTrade(Element e) {
		// TODO Auto-generated method stub
	}

	private Option readOptionTrade(Element e) {
		// multiplier
		Option.Type type = switch (e.getAttribute("putCall")) {
		case "C" -> Option.Type.Call;
		case "P" -> Option.Type.Put;
		default -> null;
		};
		String underlyingSymbol = e.getAttribute("underlyingSymbol");
		int underlyingConid = Integer.parseInt(e.getAttribute("underlyingConid"));
		String underlyingExchange = e.getAttribute("underlyingListingExchange"); 
		if (StringUtils.isBlank(underlyingExchange)) {
			underlyingExchange = e.getAttribute("listingExchange");
		}
		BigDecimal strike = new BigDecimal(e.getAttribute("strike"));
		LocalDate expiryDate = LocalDate.parse(e.getAttribute("expiry"), ibDateFormatter);
		Currency currency = Currency.getInstance(e.getAttribute("currency"));

		return new Option(type, underlyingSymbol, underlyingConid, underlyingExchange, strike, 
				currency, Day.from(expiryDate));
	}

	private void readCashTrade(Element e) {
		// TODO Auto-generated method stub
	}

}
