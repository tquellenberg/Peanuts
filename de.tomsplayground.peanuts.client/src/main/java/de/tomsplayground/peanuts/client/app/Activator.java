package de.tomsplayground.peanuts.client.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import de.tomsplayground.peanuts.app.quicken.QifReader;
import de.tomsplayground.peanuts.client.util.PeanutsAdapterFactory;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.INamedElement;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.persistence.Persistence;
import de.tomsplayground.peanuts.persistence.xstream.PersistenceService;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	public static final String IMAGE_SECURITY = "security";
	public static final String IMAGE_SECURITYCATEGORY = "security_category";
	public static final String IMAGE_ACCOUNT = "account";
	public static final String IMAGE_CATEGORY = "category";
	public static final String IMAGE_CATEGORY_INCOME = "category_income";
	public static final String IMAGE_CATEGORY_EXPENSE = "category_expense";
	public static final String IMAGE_CALENDAR = "calendar";
	public static final String IMAGE_REPORT = "report";
	public static final String IMAGE_FORECAST = "forecast";
	public static final String IMAGE_CREDIT = "credit";

	private static final String COM_TQ_FILENAME = "com.tq.filename";

	private static final String ALGORITHM = "PBEWithMD5AndDES";
	private static final byte[] SALT = new byte[]{0x3f, 0x5e, 0x7a, 0x56, 0x35, 0x57, 0x71, 0x59};

	public static final String TRANSFER_PREFIX = "=>";

	// The plug-in ID
	public static final String PLUGIN_ID = "de.tomsplayground.peanuts.client";

	// The shared instance
	private static Activator plugin;

	private AccountManager accountManager;
	private String passphrase;
	private static ColorProvider colorProvider;

	/**
	 * The constructor
	 */
	public Activator() {
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		
		Platform.getAdapterManager().registerAdapters(new PeanutsAdapterFactory(), INamedElement.class);
		Platform.getAdapterManager().registerAdapters(new PeanutsAdapterFactory(), IEditorInput.class);
		
		File dir = getStateLocation().append("securityprices").toFile();
		if (! dir.exists()) {
			dir.mkdir();
		}
		getPreferenceStore().setDefault("securitypricepath", dir.getAbsolutePath());

		PriceProviderFactory.setLocalPriceStorePath(getPreferenceStore().getString("securitypricepath"));
		
		getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {			
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				System.out
						.println("Activator.start(...).new IPropertyChangeListener() {...}.propertyChange()" + event);
				System.out.println(event.getProperty());
				System.out.println(event.getNewValue());
				if (event.getProperty().equals("securitypricepath")) {
					PriceProviderFactory.setLocalPriceStorePath((String)event.getNewValue());
				}
			}
		});
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry registry) {
		getImageRegistry().put(IMAGE_ACCOUNT, getImageDescriptor("/icons/table.png"));
		getImageRegistry().put(IMAGE_SECURITY, getImageDescriptor("/icons/chart_curve.png"));
		getImageRegistry().put(IMAGE_SECURITYCATEGORY, getImageDescriptor("/icons/chart_pie.png"));
		getImageRegistry().put(IMAGE_CATEGORY, getImageDescriptor("/icons/folder.png"));
		getImageRegistry().put(IMAGE_CATEGORY_INCOME, getImageDescriptor("/icons/folder_add.png"));
		getImageRegistry().put(IMAGE_CATEGORY_EXPENSE, getImageDescriptor("/icons/folder_delete.png"));
		getImageRegistry().put(IMAGE_CALENDAR, getImageDescriptor("icons/calendar.png"));
		getImageRegistry().put(IMAGE_REPORT, getImageDescriptor("icons/book.png"));
		getImageRegistry().put(IMAGE_FORECAST, getImageDescriptor("icons/weather_cloudy.png"));
		getImageRegistry().put(IMAGE_CREDIT, getImageDescriptor("icons/script.png"));
	}

	public synchronized ColorProvider getColorProvider() {
		if (colorProvider == null)
			colorProvider = new ColorProvider(getWorkbench().getDisplay());
		return colorProvider;
	}
	
	private OutputStream writeSecure(String passphrase, File file) {
		try {
			int iterations = 20;
			PBEKeySpec keySpec = new PBEKeySpec(passphrase.toCharArray());
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
			SecretKey secret = keyFactory.generateSecret(keySpec);
			PBEParameterSpec pbeParameterSpec = new PBEParameterSpec(SALT, iterations);
			
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, secret, pbeParameterSpec);
	
			return new CipherOutputStream(new FileOutputStream(file), cipher);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	private InputStream readSecure(String passphrase, File file) {
		try {
			int iterations = 20;
			PBEKeySpec keySpec = new PBEKeySpec(passphrase.toCharArray());
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
			SecretKey secret = keyFactory.generateSecret(keySpec);
			PBEParameterSpec pbeParameterSpec = new PBEParameterSpec(SALT, iterations);
			
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, secret, pbeParameterSpec);
	
			return new CipherInputStream(new FileInputStream(file), cipher);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void load(String filename) throws IOException {
		// Load file
		Persistence persistence = new Persistence();
		persistence.setPersistenceService(new PersistenceService());
		Reader reader;
		if (new File(filename+"s").canRead()) {
			System.out.println("Activator.load() SECURE");
			reader = new InputStreamReader(readSecure(passphrase, new File(filename+"s")), "UTF-8");			
		} else {
			System.out.println("Activator.load() UNSECURE");
			reader = new InputStreamReader(new FileInputStream(new File(filename)), "UTF-8");
		}
		accountManager = persistence.read(reader);
		IOUtils.closeQuietly(reader);
		// Save filename in pref store
		getPreferenceStore().setValue(COM_TQ_FILENAME, filename);
		((IPersistentPreferenceStore) getPreferenceStore()).save();
	}
	
	public void save(String filename) throws IOException {
		// Save copy of old file
		if (new File(filename+"s").exists()) {
			File bakFile = new File(filename+"s.bak");
			bakFile.delete();
			FileUtils.copyFile(new File(filename+"s"), bakFile);
		}
		// Save data
		Persistence persistence = new Persistence();
		persistence.setPersistenceService(new PersistenceService());
		Writer securewriter = new OutputStreamWriter(writeSecure(passphrase, new File(filename+"s")), "UTF-8");
		persistence.write(securewriter, accountManager);
		IOUtils.closeQuietly(securewriter);
		// Save filename in pref store
		getPreferenceStore().setValue(COM_TQ_FILENAME, filename);
		((IPersistentPreferenceStore) getPreferenceStore()).save();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		if (colorProvider != null)
			colorProvider.dispose();
		save(getPreferenceStore().getString(COM_TQ_FILENAME));
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public AccountManager getAccountManager() {
		if (accountManager == null) {
			System.out.println("Activator.getAccountManager()");
			String filename = getPreferenceStore().getString(COM_TQ_FILENAME);
			System.out.println("Activator.getAccountManager()" + filename);
			if (filename.length() > 0) {
				try {
					load(filename);
				} catch (Exception e) {
					e.printStackTrace();
					accountManager = new AccountManager();
				}
			} else {
				accountManager = new AccountManager();
				URL url = AccountManager.class.getResource("/example.QIF");
				InputStream inStream = null;
				try {
					inStream = url.openStream();
					QifReader reader = new QifReader();
					reader.setAccountManager(accountManager);
					reader.read(new InputStreamReader(inStream, "ISO-8859-1"));
					getPreferenceStore().setValue(COM_TQ_FILENAME, "example.bpx");
					((IPersistentPreferenceStore) getPreferenceStore()).save();
				} catch (IOException e) {
					throw new RuntimeException(e);
				} finally {
					IOUtils.closeQuietly(inStream);
				}
			}
		}
		return accountManager;
	}

	public String[] getCurrencies() {
		return new String[] { "EUR", "DEM", "USD" };
	}

	public void setAccountManager(AccountManager manager) {
		this.accountManager = manager;
	}

	public void setPassPhrase(String password) {
		this.passphrase = password;
	}
}
