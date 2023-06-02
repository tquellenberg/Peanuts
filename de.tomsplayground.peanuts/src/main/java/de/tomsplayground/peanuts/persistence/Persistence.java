package de.tomsplayground.peanuts.persistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.apache.commons.io.IOUtils;

import de.tomsplayground.peanuts.domain.base.AccountManager;

public class Persistence {

	private static final int ITERATIONS = 20;
	private static final String ALGORITHM = "PBEWithMD5AndDES";
	private static final byte[] SALT = new byte[] { 0x3f, 0x5e, 0x7a, 0x56, 0x35, 0x57, 0x71, 0x59 };

	IPersistenceService persistenceService;

	public void setPersistenceService(IPersistenceService persistence) {
		this.persistenceService = persistence;
	}

	public static Reader secureReader(File file, String passphrase) {
		try {
			PBEKeySpec keySpec = new PBEKeySpec(passphrase.toCharArray());
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
			SecretKey secret = keyFactory.generateSecret(keySpec);
			PBEParameterSpec pbeParameterSpec = new PBEParameterSpec(SALT, ITERATIONS);

			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, secret, pbeParameterSpec);

			return new InputStreamReader(new CipherInputStream(new FileInputStream(file), cipher),
					StandardCharsets.UTF_8);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static Writer secureWriter(File file, String passphrase) {
		try {
			PBEKeySpec keySpec = new PBEKeySpec(passphrase.toCharArray());
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
			SecretKey secret = keyFactory.generateSecret(keySpec);
			PBEParameterSpec pbeParameterSpec = new PBEParameterSpec(SALT, ITERATIONS);

			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, secret, pbeParameterSpec);

			return new OutputStreamWriter(new CipherOutputStream(new FileOutputStream(file), cipher),
					StandardCharsets.UTF_8);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static String updateConcurrentHashMap(String xml) {
		Matcher matcher = Pattern.compile(
				"<displayConfiguration class=\"java.util.concurrent.ConcurrentHashMap\" id=\"([0-9]*)\" [^>]*>(.*?)</displayConfiguration>")
				.matcher(xml);
		StringBuffer result = new StringBuffer();
		while (matcher.find()) {
			String id = matcher.group(1);
			String mapContent = matcher.group(2);

			String newMap = "";
			Matcher matcher2 = Pattern.compile("<string>([^>]*)</string><string>([^>]*)</string>").matcher(mapContent);
			while (matcher2.find()) {
				newMap += "<entry><string>" + matcher2.group(1) + "</string><string>" + matcher2.group(2)
						+ "</string></entry>";
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

	public void write(Writer writer, AccountManager accountManager) {
		persistenceService.write(accountManager, writer);
	}

}
