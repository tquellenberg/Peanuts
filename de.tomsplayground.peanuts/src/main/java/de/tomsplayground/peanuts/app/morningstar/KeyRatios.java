package de.tomsplayground.peanuts.app.morningstar;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import au.com.bytecode.opencsv.CSVReader;
import de.tomsplayground.peanuts.domain.fundamental.FundamentalData;

public class KeyRatios {

	private final static Logger log = LoggerFactory.getLogger(KeyRatios.class);

	private final static String URL = "http://financials.morningstar.com/ajax/exportKR2CSV.html?&callback=?&t=SYMBOL&region=usa&culture=en-US&cur=&order=asc";

	public static void main(String[] args) {
		List<FundamentalData> data = new KeyRatios().readUrl("XNAS:VIAB");
		System.out.println(ToStringBuilder.reflectionToString(data));
	}

	public List<FundamentalData> readUrl(String symbol) {
		InputStreamReader reader = null;
		URL url = null;
		try {
			url = new URL(StringUtils.replace(URL, "SYMBOL", symbol));
			reader = new InputStreamReader(url.openStream(), "UTF-8");
			return readFile(reader);
		} catch (IOException e) {
			log.error("Problem with '"+symbol+ "' " + url, e);
		} catch (RuntimeException e) {
			log.error("Problem with '"+symbol+ "' " + url, e);
			throw e;
		} finally {
			IOUtils.closeQuietly(reader);
		}
		return Lists.newArrayList();
	}

	public List<FundamentalData> readFile(Reader reader) {
		CSVReader csvReader = null;
		try {
			csvReader = new CSVReader(reader);

			List<String[]> allLines = csvReader.readAll();

			String[] years = allLines.get(2);
			Preconditions.checkArgument(StringUtils.isBlank(years[0]));
			Preconditions.checkArgument(years.length == 12);

			String[] earnings = null;
			String[] dividende = null;
			for (String[] line : allLines) {
				if (StringUtils.startsWith(line[0], "Earnings Per Share")) {
					earnings = line;
				}
				if (StringUtils.startsWith(line[0], "Dividends")) {
					dividende = line;
				}
			}
			Preconditions.checkNotNull(earnings);
			Preconditions.checkNotNull(dividende);

			return parse(years, earnings, dividende);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(csvReader);
		}
		return Lists.newArrayList();
	}

	private List<FundamentalData> parse(String[] years, String[] earnings, String[] dividende) {
		List<FundamentalData> fundamentalDatas = Lists.newArrayList();
		for (int i = 1; i <= 10; i++) {
			FundamentalData fundamentalData = new FundamentalData();
			int year = Integer.parseInt(StringUtils.split(years[i], '-')[0]);
			fundamentalData.setYear(year);
			int fiscalOffset = Integer.parseInt(StringUtils.split(years[i], '-')[1]) - 12;
			fundamentalData.setFicalYearEndsMonth(fiscalOffset);
			try {
				BigDecimal earning = new BigDecimal(earnings[i]);
				fundamentalData.setEarningsPerShare(earning);
			} catch (NumberFormatException e) {
				// Okay
			}
			try {
				BigDecimal dividend = new BigDecimal(dividende[i]);
				fundamentalData.setDividende(dividend);
			} catch (NumberFormatException e) {
				// Okay
			}
			fundamentalDatas.add(fundamentalData);
		}
		return fundamentalDatas;
	}

}
