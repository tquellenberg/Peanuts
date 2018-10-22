package de.tomsplayground.peanuts.app.morningstar;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPather;
import org.htmlcleaner.XPatherException;

public class BasicData {

	private final static String URL = "http://financials.morningstar.com/company-profile/component.action?component=BasicData&t=SYMBOL&region=usa&culture=en-US&cur=";

	public static void main(String[] args) {
		SectorIndustry sectorIndustry = new BasicData().readUrl("XNAS:VIAB");
		System.out.println(sectorIndustry);
	}

	public SectorIndustry readUrl(String symbol) {
		try (InputStream in = new URL(StringUtils.replace(URL, "SYMBOL", symbol)).openStream()) {
			return readFile(new InputStreamReader(in, StandardCharsets.UTF_8));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return SectorIndustry.UNKNOWN;
	}

	public SectorIndustry readFile(Reader reader) throws IOException {
		try {
			HtmlCleaner htmlCleaner = new HtmlCleaner();
			TagNode cleanHtml = htmlCleaner.clean(reader);
			XPather xPather = new XPather("//table/tbody/tr[6]/td[3]/text()");
			Object[] result = xPather.evaluateAgainstNode(cleanHtml);
			String sector = "";
			String industry = "";
			if (result.length > 0) {
				sector = result[0].toString();
			}
			xPather = new XPather("//table/tbody/tr[6]/td[5]/text()");
			result = xPather.evaluateAgainstNode(cleanHtml);
			if (result.length > 0) {
				industry = result[0].toString();
			}
			return new SectorIndustry(sector, industry);
		} catch (XPatherException e) {
			e.printStackTrace();
		}
		return SectorIndustry.UNKNOWN;
	}
}
