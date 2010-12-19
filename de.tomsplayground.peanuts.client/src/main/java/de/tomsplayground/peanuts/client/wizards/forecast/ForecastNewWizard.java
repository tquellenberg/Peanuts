package de.tomsplayground.peanuts.client.wizards.forecast;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.reporting.forecast.Forecast;

public class ForecastNewWizard extends Wizard implements INewWizard {

	public static final String ID = "de.tomsplayground.peanuts.client.forecastNewWizard";
	
	private BasePage basePage;
	
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// nothing to do
	}
	
	@Override
	public void addPages() {
		basePage = new BasePage("Forecast");
		addPage(basePage);
	}

	@Override
	public boolean performFinish() {
		Forecast forecast = basePage.getForecaste();
		if (forecast != null) {
			Activator.getDefault().getAccountManager().addForecast(forecast);
		}
		return forecast != null;
	}


}
