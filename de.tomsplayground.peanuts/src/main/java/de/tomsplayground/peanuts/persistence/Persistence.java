package de.tomsplayground.peanuts.persistence;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import de.tomsplayground.peanuts.domain.base.AccountManager;

public class Persistence {

	IPersistenceService persistenceService;

	public void setPersistenceService(IPersistenceService persistence) {
		this.persistenceService = persistence;
	}

	public void write(Writer writer, AccountManager accountManager) {
		try {
			writer.write(persistenceService.write(accountManager));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String updateConcurrentHashMap(String xml) {
		Matcher matcher = Pattern.compile("<displayConfiguration class=\"java.util.concurrent.ConcurrentHashMap\" id=\"([0-9]*)\" [^>]*>(.*?)</displayConfiguration>").matcher(xml);
		StringBuffer result = new StringBuffer();
		while (matcher.find()) {
			String id = matcher.group(1);
			String mapContent = matcher.group(2);

			String newMap = "";
			Matcher matcher2 = Pattern.compile("<string>([^>]*)</string><string>([^>]*)</string>").matcher(mapContent);
			while (matcher2.find()) {
				newMap += "<entry><string>"+matcher2.group(1)+"</string><string>"+matcher2.group(2)+"</string></entry>";
			}
			newMap = "<displayConfiguration id=\"" + id + "\">" + newMap + "</displayConfiguration>";
			matcher.appendReplacement(result, newMap);
		}
		matcher.appendTail(result);
		return result.toString();
	}

	public AccountManager read(Reader reader) {
		try {
			String xml = IOUtils.toString(reader);
			String xml2 = updateConcurrentHashMap(xml);
			AccountManager readAccountManager = persistenceService.readAccountManager(xml2);
			readAccountManager.reconfigureAfterDeserialization();
			return readAccountManager;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
