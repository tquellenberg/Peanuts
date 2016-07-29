package de.tomsplayground.peanuts.app.yahoo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPather;
import org.htmlcleaner.XPatherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class YahooSecuritySearcher {

	private final static Logger log = LoggerFactory.getLogger(YahooSecuritySearcher.class);

	private static final String URL = "http://finance.yahoo.com/lookup/stocks?s={}&t=S&m=ALL&r=";

	public List<YahooSecurity> search(String query) {
		List<YahooSecurity> result = Lists.newArrayList();
		String queryEnc = "";
		try {
			queryEnc = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
		}
		String url = StringUtils.replace(URL, "{}", queryEnc);
		log.info("YahooSecuritySearcher.search(): " + url);
		try {
			HtmlCleaner cleaner = new HtmlCleaner();
			TagNode node = cleaner.clean(new URL(url));
			XPather xPather = new XPather("//div[@id='yfi_sym_results']/table/tbody/tr");
			Object[] node2 = xPather.evaluateAgainstNode(node);
			if (node2 != null && node2.length > 0) {
				for (Object object : node2) {
					if (object instanceof TagNode) {
						TagNode tr = (TagNode) object;
						List<TagNode> childTagList = tr.getChildTagList();
						if (childTagList.size() == 6) {
							CharSequence symbol = childTagList.get(0).getText();
							CharSequence name = childTagList.get(1).getText();
							CharSequence exchange = childTagList.get(5).getText();
							result.add(new YahooSecurity(symbol.toString(), name.toString(), exchange.toString()));
						}
					}
				}
			} else {
				log.info("No result found for {}", query);
			}
		} catch (IOException e) {
			log.error("", e);
		} catch (XPatherException e) {
			log.error("", e);
		}
		return result;
	}

}
