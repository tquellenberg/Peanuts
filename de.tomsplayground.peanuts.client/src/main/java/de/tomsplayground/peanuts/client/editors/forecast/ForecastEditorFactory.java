package de.tomsplayground.peanuts.client.editors.forecast;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

import de.tomsplayground.peanuts.client.app.Activator;
import de.tomsplayground.peanuts.domain.base.AccountManager;
import de.tomsplayground.peanuts.domain.reporting.forecast.Forecast;

public class ForecastEditorFactory implements IElementFactory {

	public static final String ID = "de.tomsplayground.peanuts.client.forecastEditorFactoryId";
	public static final String FORECAST_NAME = "forecast.name";

	@Override
	public IAdaptable createElement(IMemento memento) {
		String name = memento.getString(FORECAST_NAME);
		AccountManager accountManager = Activator.getDefault().getAccountManager();
		Forecast forecast = accountManager.getForecast(name);
		if (forecast == null) {
			forecast = new Forecast(name);
			accountManager.addForecast(forecast);
		}
		return new ForecastEditorInput(forecast);
	}

}
