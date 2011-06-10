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
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import de.tomsplayground.peanuts.client.util.PeanutsAdapterFactory;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.INamedElement;
import de.tomsplayground.peanuts.domain.base.InventoryEntry;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.persistence.Persistence;
import de.tomsplayground.peanuts.persistence.xstream.PersistenceService;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	private static final String EXAMPLE_FILENAME = "example.bpx";
	private static final String EXAMPLE = "<EXAMPLE>";
	
	public static final String FILE_EXTENSION_XML = "bpx";
	public static final String FILE_EXTENSION_SECURE = "bps";
	public static final String[] ALL_FILE_PATTERN = new String[]{"*."+FILE_EXTENSION_SECURE, "*."+FILE_EXTENSION_XML};
	
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
	public static final String IMAGE_LOAD_FILE = "load_file";

	public static final String FILENAME_PROPERTY = "com.tq.filename";
	private static final String SECURITYPRICEPATH_PROPERTY = "securitypricepath";

	private static final int ITERATIONS = 20;
	private static final String ALGORITHM = "PBEWithMD5AndDES";
	private static final byte[] SALT = new byte[]{0x3f, 0x5e, 0x7a, 0x56, 0x35, 0x57, 0x71, 0x59};

	public static final String TRANSFER_PREFIX = "=>";

	// The plug-in ID
	public static final String PLUGIN_ID = "de.tomsplayground.peanuts.client";

	// The shared instance
	private static Activator plugin;
	private static ColorProvider colorProvider;

	private AccountManager accountManager;
	private String passphrase;

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
		Platform.getAdapterManager().registerAdapters(new PeanutsAdapterFactory(), InventoryEntry.class);
		
		File dir = getStateLocation().append("securityprices").toFile();
		if (! dir.exists()) {
			dir.mkdir();
		}
		getPreferenceStore().setDefault(SECURITYPRICEPATH_PROPERTY, dir.getAbsolutePath());

		PriceProviderFactory.setLocalPriceStorePath(getPreferenceStore().getString(SECURITYPRICEPATH_PROPERTY));
		
		getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {			
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().equals(SECURITYPRICEPATH_PROPERTY)) {
					PriceProviderFactory.setLocalPriceStorePath((String)event.getNewValue());
				}
			}
		});

		if (StringUtils.isBlank(getFilename())) {
			setFilename(EXAMPLE);
		}
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
		getImageRegistry().put(IMAGE_LOAD_FILE, getImageDescriptor("icons/database_go.png"));
	}

	public synchronized ColorProvider getColorProvider() {
		if (colorProvider == null)
			colorProvider = new ColorProvider(getWorkbench().getDisplay());
		return colorProvider;
	}
	
	private OutputStream writeSecure(File file) {
		try {
			PBEKeySpec keySpec = new PBEKeySpec(passphrase.toCharArray());
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
			SecretKey secret = keyFactory.generateSecret(keySpec);
			PBEParameterSpec pbeParameterSpec = new PBEParameterSpec(SALT, ITERATIONS);
			
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, secret, pbeParameterSpec);
	
			return new CipherOutputStream(new FileOutputStream(file), cipher);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	private InputStream readSecure(File file) {
		try {
			PBEKeySpec keySpec = new PBEKeySpec(passphrase.toCharArray());
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
			SecretKey secret = keyFactory.generateSecret(keySpec);
			PBEParameterSpec pbeParameterSpec = new PBEParameterSpec(SALT, ITERATIONS);
			
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
		Reader reader;
		if (filename.equals(EXAMPLE)) {
			reader = new InputStreamReader(AccountManager.class.getResourceAsStream("/"+EXAMPLE_FILENAME), "UTF-8");
		} else {
			File file = new File(filename);
			if (! file.exists()) {
				setFilename(filename);				
				accountManager = new AccountManager();
				return;
			}
			if (filename.endsWith("."+FILE_EXTENSION_SECURE)) {
				reader = new InputStreamReader(readSecure(file), "UTF-8");
			} else {
				reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
			}
		}
		try {
			Persistence persistence = new Persistence();
			persistence.setPersistenceService(new PersistenceService());
			accountManager = persistence.read(reader);
			if (filename.equals(EXAMPLE)) {
				setFilename(System.getProperty("user.home") + File.separator + EXAMPLE_FILENAME);
			} else {
				setFilename(filename);				
			}
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}
	
	public void save(String filename) throws IOException {
		// Save copy of old file
		File file = new File(filename);
		if (file.exists()) {
			File bakFile = new File(filename+".bak");
			bakFile.delete();
			FileUtils.copyFile(file, bakFile);
		}
		Writer writer;
		if (filename.endsWith("."+FILE_EXTENSION_SECURE)) {
			writer = new OutputStreamWriter(writeSecure(file), "UTF-8");
		} else {
			writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
		}
		// Save data
		Persistence persistence = new Persistence();
		persistence.setPersistenceService(new PersistenceService());
		persistence.write(writer, accountManager);
		IOUtils.closeQuietly(writer);
		setFilename(filename);
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
		if (accountManager != null) {
			save(getFilename());
		}
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

	private void setFilename(String filename) throws IOException {
		getPreferenceStore().setValue(FILENAME_PROPERTY, filename);
		((IPersistentPreferenceStore) getPreferenceStore()).save();
	}
	
	public String getFilename() {
		return getPreferenceStore().getString(FILENAME_PROPERTY);
	}
}
