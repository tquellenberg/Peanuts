package de.tomsplayground.peanuts.client.editors.forecast;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

import de.tomsplayground.peanuts.domain.reporting.forecast.Forecast;

public class ForecastEditorInput implements IEditorInput {

	private Forecast forecast;
	
	public ForecastEditorInput(Forecast forecast) {
		this.forecast = forecast;
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
		return forecast.getName();
	}

	@Override
	public IPersistableElement getPersistable() {
		return new IPersistableElement() {

			@Override
			public String getFactoryId() {
				return ForecastEditorFactory.ID;
			}

			@Override
			public void saveState(IMemento memento) {
				memento.putString(ForecastEditorFactory.FORECAST_NAME, forecast.getName());
			}
		};
	}

	@Override
	public String getToolTipText() {
		return forecast.getName();
	}

	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		return null;
	}

	public Forecast getForecast() {
		return forecast;
	}

}
