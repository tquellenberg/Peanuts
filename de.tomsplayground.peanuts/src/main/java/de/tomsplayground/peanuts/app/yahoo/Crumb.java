package de.tomsplayground.peanuts.app.yahoo;

import java.util.List;

import org.apache.http.cookie.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Crumb {

	private final static Logger log = LoggerFactory.getLogger(Crumb.class);

	private final String id;
	private final List<Cookie> cookies;

	public Crumb(String id, List<Cookie> cookies) {
		this.id = id;
		this.cookies = cookies;
		for (Cookie cookie : cookies) {
			log.info("Crumb-Cookie: {}", cookie);
		}
	}

	public String getId() {
		return id;
	}

	public List<Cookie> getCookies() {
		return cookies;
	}
}
