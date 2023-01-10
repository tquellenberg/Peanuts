package de.tomsplayground.peanuts.client.app;

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
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.service.application.ApplicationHandle;

import de.tomsplayground.peanuts.client.savedtransaction.SavedTransactionManager;
import de.tomsplayground.peanuts.client.util.PeanutsAdapterFactory;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.currenncy.ExchangeRates;
import de.tomsplayground.peanuts.domain.process.PriceProviderFactory;
import de.tomsplayground.peanuts.persistence.Persistence;
import de.tomsplayground.peanuts.persistence.xstream.PersistenceService;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	private static final String EXAMPLE_FILENAME = "example.bpx";
	private static final String EXAMPLE = "<EXAMPLE>";

	public static final String RED = "RED";
	public static final String GREEN = "GREEN";
	public static final String RED_BG = "RED_BG";
	public static final String GREEN_BG = "GREEN_BG";
	public static final String GRAY_BG = "GRAY_BG";
	public static final String ACTIVE_ROW = "ACTIVE_ROW";
	public static final String INACTIVE_ROW = "INACTIVE_ROW";

	public static final String FILE_EXTENSION_XML = "bpx";
	public static final String FILE_EXTENSION_SECURE = "bps";
	public static final String[] ALL_FILE_PATTERN = new String[]{"*."+FILE_EXTENSION_SECURE, "*."+FILE_EXTENSION_XML};

	public static final String IMAGE_SECURITY = "security";
	public static final String IMAGE_SECURITYCATEGORY = "security_category";
	public static final String IMAGE_SAVED_TRANSACTION = "saved_transaction";
	public static final String IMAGE_COMPARISON = "comparison";
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
	public static final String SECURITYPRICEPATH_PROPERTY = "securitypricepath";

	public static final String RAPIDAPIKEY_PROPERTY = "rapidapikey";


	public static final String TRANSFER_PREFIX = "=>";

	// The plug-in ID
	public static final String PLUGIN_ID = "de.tomsplayground.peanuts.client";

	// The shared instance
	private static Activator plugin;
	private static ColorRegistry colorProvider;

	private AccountManager accountManager;
	private ExchangeRates exchangeRates;
	private String passphrase;

	/**
	 * The constructor
	 */
	public Activator() {
		synchronized (Activator.class) {
			plugin = this;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);

		for (Class<?> c : PeanutsAdapterFactory.getAdaptableClasses()) {
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

	private void initFonts() {
		FontRegistry fontRegistry = JFaceResources.getFontRegistry();
		FontData[] fontDatas = fontRegistry.getFontData(JFaceResources.DEFAULT_FONT);
		if (fontDatas.length > 0) {
			FontData fontData = fontDatas[0];
			fontRegistry.put("de.tomsplayground.fonts.default", new FontData[]{
					new FontData(fontData.getName(), 12 , fontData.getStyle())});
			fontRegistry.put("de.tomsplayground.fonts.small", new FontData[]{
					new FontData(fontData.getName(), 11 , fontData.getStyle())});
		}
	}

	public Font getNormalFont() {
		FontRegistry fontRegistry = JFaceResources.getFontRegistry();
		if (! fontRegistry.hasValueFor("de.tomsplayground.fonts.default")) {
			initFonts();
		}
		return fontRegistry.get("de.tomsplayground.fonts.default");
	}
	
	public Font getSmallFont() {
		FontRegistry fontRegistry = JFaceResources.getFontRegistry();
		if (! fontRegistry.hasValueFor("de.tomsplayground.fonts.small")) {
			initFonts();
		}
		return fontRegistry.get("de.tomsplayground.fonts.small");
	}

	protected void applicationStopping() {
	}

	protected void applicationStarted() {
	}

	public ExchangeRates getExchangeRates() {
		if (exchangeRates == null) {
			exchangeRates = new ExchangeRates(PriceProviderFactory.getInstance(), accountManager);
		}
		return exchangeRates;
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry registry) {
		ImageRegistry imageRegistry = getImageRegistry();
		imageRegistry.put(IMAGE_ACCOUNT, getImageDescriptor("/icons/table.png"));
		imageRegistry.put(IMAGE_SECURITY, getImageDescriptor("/icons/chart_curve.png"));
		imageRegistry.put(IMAGE_SECURITYCATEGORY, getImageDescriptor("/icons/chart_pie.png"));
		imageRegistry.put(IMAGE_CATEGORY, getImageDescriptor("/icons/folder.png"));
		imageRegistry.put(IMAGE_CATEGORY_INCOME, getImageDescriptor("/icons/folder_add.png"));
		imageRegistry.put(IMAGE_CATEGORY_EXPENSE, getImageDescriptor("/icons/folder_delete.png"));
		imageRegistry.put(IMAGE_CALENDAR, getImageDescriptor("icons/calendar.png"));
		imageRegistry.put(IMAGE_REPORT, getImageDescriptor("icons/book.png"));
		imageRegistry.put(IMAGE_FORECAST, getImageDescriptor("icons/weather_cloudy.png"));
		imageRegistry.put(IMAGE_CREDIT, getImageDescriptor("icons/script.png"));
		imageRegistry.put(IMAGE_LOAD_FILE, getImageDescriptor("icons/database_go.png"));
		imageRegistry.put(IMAGE_SAVED_TRANSACTION, getImageDescriptor("icons/date.png"));
		imageRegistry.put(IMAGE_COMPARISON, getImageDescriptor("icons/arrow_refresh.png"));
	}

	public Image getImage(String path) {
		ImageRegistry imageRegistry = getImageRegistry();
		ImageDescriptor imageDescriptor = imageRegistry.getDescriptor(path);
		if (imageDescriptor == null) {
			imageDescriptor = getImageDescriptor(path);
			imageRegistry.put(path, imageDescriptor);
		}
		return imageRegistry.get(path);
	}

	public synchronized ColorRegistry getColorProvider() {
		if (colorProvider == null) {
			colorProvider = new ColorRegistry(PlatformUI.getWorkbench().getDisplay());
			colorProvider.put(RED, new RGB(0xFF, 0x0D, 0x00));
			colorProvider.put(GREEN, new RGB(0x00, 0xC6, 0x18));
			colorProvider.put(RED_BG, new RGB(0xFF, 0x7A, 0x73));
			colorProvider.put(GREEN_BG, new RGB(0x72, 0xEE, 0x81));
			colorProvider.put(ACTIVE_ROW, new RGB(0x38, 0x59, 0xBA));
			colorProvider.put(INACTIVE_ROW, new RGB(0xB7, 0xBB, 0xC7));
			colorProvider.put(GRAY_BG, new RGB(0xAA, 0xAA, 0xAA));
		}
		return colorProvider;
	}

	public static File getLockFile(String filename) {
		return new File(filename+".lock");
	}

	private void createLockFile(String filename) {
		try {
			getLockFile(filename).createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void removeLockFile() {
		FileUtils.deleteQuietly(getLockFile(getFilename()));
	}


	public void load(String filename) throws IOException {
		File file = new File(filename);
		if (! StringUtils.equals(filename, EXAMPLE) && ! file.exists()) {
			// New empty file
			setFilename(filename);
			accountManager = new AccountManager();
			return;
		}

		try (Reader reader = getReader(filename)) {
			Persistence persistence = new Persistence();
			persistence.setPersistenceService(new PersistenceService());
			accountManager = persistence.read(reader);
			if (filename.equals(EXAMPLE)) {
				setFilename(System.getProperty("user.home") + File.separator + EXAMPLE_FILENAME);
			} else {
				setFilename(filename);
				createLockFile(filename);
			}
		}

		try {
			SavedTransactionManager.createFuturTransactions(90);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private Reader getReader(String filename) throws FileNotFoundException {
		Reader reader;
		if (filename.equals(EXAMPLE)) {
			reader = new InputStreamReader(AccountManager.class.getResourceAsStream("/"+EXAMPLE_FILENAME), StandardCharsets.UTF_8);
		} else {
			File file = new File(filename);
			if (filename.endsWith("."+FILE_EXTENSION_SECURE)) {
				reader = Persistence.secureReader(file, passphrase);
			} else {
				reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
			}
		}
		return reader;
	}

	public void save(String filename) throws IOException {
		// Save copy of old file
		File file = new File(filename);
		if (file.exists()) {
			File bakFile = new File(filename+".bak");
			bakFile.delete();
			FileUtils.copyFile(file, bakFile);
		}

		// Save data
		try (Writer writer = getWriter(filename)) {
			Persistence persistence = new Persistence();
			persistence.setPersistenceService(new PersistenceService());
			persistence.write(writer, accountManager);
			setFilename(filename);
		}
	}

	private Writer getWriter(String filename) throws FileNotFoundException {
		Writer writer;
		File file = new File(filename);
		if (filename.endsWith("."+FILE_EXTENSION_SECURE)) {
			writer = Persistence.secureWriter(file, passphrase);
		} else {
			writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
		}
		return writer;
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
			removeLockFile();
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

//	public IbConnection getIbConnection() {
//		return ibConnection;
//	}
}
