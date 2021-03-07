package de.tomsplayground.peanuts.client.comparison;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.google.common.collect.Lists;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.base.Security;
import de.tomsplayground.peanuts.util.Day;

public class ComparisonInput implements IEditorInput {

	public final static List<Day> START_DAYS = Lists.newArrayList(new Day(2020, 1, 1), new Day(2020, 2, 23),
		new Day(2020, 8, 2), new Day(2020, 9, 30), new Day(2021, 0, 1), Day.today().addMonth(-1), Day.today().addDays(-14));

	private final List<String> isins = Lists.newArrayList("XLC", "XLY", "XLP", "XLE", "XLF", "XLV", "XLI", "XLB", "XLRE", "XLK", "XLU", "SPY");

	private final List<Security> securities = new ArrayList<>();

	private Day startDate = new Day(2020, 1, 1);

	public ComparisonInput() {
		AccountManager accountManager = Activator.getDefault().getAccountManager();
		accountManager.getSecurities().stream()
			.filter(s -> isins.contains(s.getISIN()))
			.forEach(s -> securities.add(s));
	}

	public List<Security> getSecurities() {
		return securities;
	}

	public Optional<Security> getBaseSecurity() {
		AccountManager accountManager = Activator.getDefault().getAccountManager();
		return accountManager.getSecurities().stream()
			.filter(s -> StringUtils.equals(s.getISIN(), "SPY"))
			.findAny();
	}

	public Day getStartDate() {
		return startDate;
	}

	public void setStartDate(Day startDate) {
		this.startDate = startDate;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageDescriptor.getMissingImageDescriptor();
	}

	@Override
	public String getName() {
		return "S+P 500 sectors";
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return getName();
	}

}
