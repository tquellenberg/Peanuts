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
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.service.application.ApplicationHandle;

import com.google.common.collect.ImmutableList;

import de.tomsplayground.peanuts.client.editors.security.properties.SecurityPropertyPage;
import de.tomsplayground.peanuts.client.util.PeanutsAdapterFactory;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.domain.currenncy.ExchangeRates;
import de.tomsplayground.peanuts.domain.process.IPriceProvider;
import de.tomsplayground.peanuts.domain.process.Price;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.persistence.Persistence;
import de.tomsplayground.peanuts.persistence.xstream.PersistenceService;
import de.tomsplayground.peanuts.scraping.Scraping;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	private static final String EXAMPLE_FILENAME = "example.bpx";
	private static final String EXAMPLE = "<EXAMPLE>";

	public static final String LIST_EVEN = "LIST_EVEN";
	public static final String LIST_ODD = "LIST_ODD";
	public static final String RED = "RED";
	public static final String GREEN = "GREEN";
	public static final String RED_BG = "RED_BG";
	public static final String GREEN_BG = "GREEN_BG";
	public static final String ACTIVE_ROW = "ACTIVE_ROW";
	public static final String INACTIVE_ROW = "INACTIVE_ROW";

	public static final String FILE_EXTENSION_XML = "bpx";
	public static final String FILE_EXTENSION_SECURE = "bps";
	public static final String[] ALL_FILE_PATTERN = new String[]{"*."+FILE_EXTENSION_SECURE, "*."+FILE_EXTENSION_XML};

	public static final String IMAGE_SECURITY = "security";
	public static final String IMAGE_SECURITYCATEGORY = "security_category";
	public static final String IMAGE_SAVED_TRANSACTION = "saved_transaction";
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
	private static ColorRegistry colorProvider;

	private AccountManager accountManager;
	private ExchangeRates exchangeRates;
	private String passphrase;
	private Job refreshPricesJob;

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

		for (@SuppressWarnings("rawtypes") Class c : PeanutsAdapterFactory.getAdaptableClasses()) {
			Platform.getAdapterManager().registerAdapters(new PeanutsAdapterFactory(), c);
		}

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
		context.addServiceListener(new ServiceListener() {
			@Override
			public void serviceChanged(ServiceEvent event) {
				if (event.getType() == ServiceEvent.MODIFIED) {
					if (Objects.equals(event.getServiceReference().getProperty("application.state"), "RUNNING")) {
						applicationStarted();
					} else if (Objects.equals(event.getServiceReference().getProperty("application.state"), "STOPPING")) {
						applicationStopping();
					}
				}
			}
		}, "(objectclass=" + ApplicationHandle.class.getName() + ")");
	}

	protected void applicationStopping() {
		if (refreshPricesJob != null) {
			refreshPricesJob.cancel();
		}
	}

	public ExchangeRates getExchangeRate() {
		if (exchangeRates == null) {
			exchangeRates = new ExchangeRates(PriceProviderFactory.getInstance(), accountManager);
		}
		return exchangeRates;
	}

	protected void applicationStarted() {
		refreshPricesJob = new Job("Refresh investment prices") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					PriceProviderFactory priceProviderFactory = PriceProviderFactory.getInstance();
					ImmutableList<Security> securities = Activator.getDefault().getAccountManager().getSecurities();
					monitor.beginTask("Refresh investment prices", securities.size());
					for (Security security : securities) {
						monitor.subTask("Refreshing " + security.getName());
						priceProviderFactory.refresh(security, Boolean.valueOf(
							security.getConfigurationValue(SecurityPropertyPage.OVERRIDE_EXISTING_PRICE_DATA)).booleanValue());
						Scraping scraping = new Scraping(security);
						Price price = scraping.execute();
						if (price != null) {
							IPriceProvider priceProvider = priceProviderFactory.getPriceProvider(security);
							priceProvider.setPrice(price);
							priceProviderFactory.saveToLocal(security, priceProvider);
						}
						monitor.worked(1);
						if (monitor.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
					}
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		refreshPricesJob.setUser(false);
		refreshPricesJob.setSystem(false);
		refreshPricesJob.schedule();
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
		getImageRegistry().put(IMAGE_SAVED_TRANSACTION, getImageDescriptor("icons/date.png"));
	}

	public Image getImage(String path) {
		ImageDescriptor imageDescriptor = getImageRegistry().getDescriptor(path);
		if (imageDescriptor == null) {
			imageDescriptor = getImageDescriptor(path);
			getImageRegistry().put(path, imageDescriptor);
		}
		return imageDescriptor.createImage();
	}

	public synchronized ColorRegistry getColorProvider() {
		if (colorProvider == null) {
			colorProvider = new ColorRegistry(getWorkbench().getDisplay());
			colorProvider.put(LIST_EVEN, new RGB(0xBF, 0xE4, 0xFF));
			colorProvider.put(LIST_ODD, new RGB(0xFF, 0xF2, 0xBF));
			colorProvider.put(RED, new RGB(0xFF, 0x0D, 0x00));
			colorProvider.put(GREEN, new RGB(0x00, 0xC6, 0x18));
			colorProvider.put(RED_BG, new RGB(0xFF, 0x7A, 0x73));
			colorProvider.put(GREEN_BG, new RGB(0x66, 0xE2, 0x75));
			colorProvider.put(ACTIVE_ROW, new RGB(0x38, 0x59, 0xBA));
			colorProvider.put(INACTIVE_ROW, new RGB(0xB7, 0xBB, 0xC7));
		}
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
